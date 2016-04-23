import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class Main_Thread extends Thread {
	
	private Config config;
	private static BufferedWriter fr_file;
	private static BufferedReader to_file;
	private static BufferedWriter rc_file;
	
	public Main_Thread(Config config) {
		super();
		this.config = config;
	}

	@Override
	public void run() {
		if(!open_files())
			return;
		for(int i = 0 ; i < 240 ; ) {
			/*read toX.txt
			process any new received messages (i.e. DATA, HELLO, TC)*/
			process_incoming_messages();
			
			/*if it is time to send the data string
			if there is a routing table entry for the destination
			send the data message*/
			boolean is_unsucceed = send_data_message(i);
			
			//if i is a multiple of 5 send a hello message
			send_hello_message(i);
			
			//if i is a multiple of 10 send a TC message
			send_tc_message(i);
			
			//remove old entries of the neighbor table if necessary
			boolean is_neighbors_changed = remove_old_neighbors();
			
			//remove old entries from the TC table if necessary
			boolean is_topology_changed = remove_old_tcs();
			
			//recalculate the routing table if necessary
			if(is_neighbors_changed || is_topology_changed)
				calculate_routing_table();
			//wait for a second
			if(is_unsucceed || i%2!=0) {
				i++;
				try_sleep(500);
			} else {
				i+=2;
				try_sleep(1000);
			}
		}
		close_files();
	}

	private void calculate_routing_table() {
		boolean is_changed = false;
		
		for(int i = 0 ; i < config.number_of_nodes() ; i++) {
			config.routing_table()[i][0] = -1;
			config.routing_table()[i][1] = Integer.MAX_VALUE;
		}
		
		for(int i = 0 ; i < config.number_of_nodes() ; i++) {
			if(config.one_hop_neighbors()[i] == State_of_Link.Bidirectional ||
					config.one_hop_neighbors()[i] == State_of_Link.MPR) {
				is_changed = true;
				config.routing_table()[i][0] = i;
				config.routing_table()[i][1] = 1;
			}
		}
		
		config.routing_table()[config.node_id()][0] = config.node_id();
		config.routing_table()[config.node_id()][1] = 0;
		
		while(is_changed) {
			is_changed = false;
			for(int i = 0 ; i < config.number_of_nodes() ; i++) {
				if(config.routing_table()[i][0] == -1)
					continue;
				int distance = config.routing_table()[i][1] + 1;
				for(int j = 0 ; j < config.number_of_nodes() ; j++)
					if(config.topology_table()[i][j]) {
						if(distance < config.routing_table()[j][1]) {
							config.routing_table()[j][0] = config.routing_table()[i][0];
							config.routing_table()[j][1] = distance;
							is_changed = true;
						}
					}
			}
		}
	}

	private boolean remove_old_tcs() {
		boolean is_topology_changed = false;
		for(int i = 0 ; i < config.number_of_nodes() ; i++)
			if(config.topology_seq_ttl()[i].holding_time() == 45) {
				is_topology_changed = true;
				remove_tc(i);
			}
		for(int i = 0 ; i < config.number_of_nodes() ; i++)
			config.topology_seq_ttl()[i].set_holding_time(config.topology_seq_ttl()[i].holding_time());
		return is_topology_changed;
	}

	private void remove_tc(int node_id) {
		config.topology_seq_ttl()[node_id] = new TC_Entry(0, 0);
		for(int j = 0 ; j < config.number_of_nodes() ; j++)
			config.topology_table()[node_id][j] = false;
	}

	private boolean remove_old_neighbors() {
		boolean is_neighbors_changed = false;
		for(int i = 0 ; i < config.number_of_nodes() ; i++)
			if(config.neighbors_ttl()[i] == 15) {
				is_neighbors_changed = true;
				remove_neighbor(i);
			}
		for(int i = 0 ; i < config.number_of_nodes() ; i++)
			config.neighbors_ttl()[i]++;
		return is_neighbors_changed;
	}

	private void remove_neighbor(int node_id) {
		config.one_hop_neighbors()[node_id] = State_of_Link.None;
		for(int i = 0 ; i < config.number_of_nodes() ; i++)
			config.two_hop_neighbors()[i][node_id] = false;
		config.ms_nodes()[node_id] = false;
	}

	private void send_tc_message(int cur_time) {
		if(cur_time % 20 != 0)
			return;
		config.set_seqno(config.seqno() + 1);
		String tc_msg = "* " + config.node_id() + " TC " + config.node_id()
							+ " " + config.seqno() + " MS";
		for(int i = 0 ; i < config.number_of_nodes() ; i++)
			if(config.ms_nodes()[i])
				tc_msg += (" " + i);
		write_to_file(fr_file, tc_msg);
	}

	private void send_hello_message(int cur_time) {
		if(cur_time % 10 != 0)
			return;
		String hello_msg = "* " + config.node_id() + " HELLO UNIDIR";
		for(int i = 0 ; i < config.number_of_nodes() ; i++) {
			if(config.one_hop_neighbors()[i] == State_of_Link.Unidirectional)
				hello_msg += (" " + i);
		}
		hello_msg += " BIDIR";
		for(int i = 0 ; i < config.number_of_nodes() ; i++) {
			if(config.one_hop_neighbors()[i] == State_of_Link.Bidirectional)
				hello_msg += (" " + i);
		}
		hello_msg += " MPR";
		for(int i = 0 ; i < config.number_of_nodes() ; i++) {
			if(config.one_hop_neighbors()[i] == State_of_Link.MPR)
				hello_msg += (" " + i);
		}
		write_to_file(fr_file, hello_msg);
	}

	private boolean send_data_message(int cur_time) {
		if(config.is_message_sent())
			return false;
		if(cur_time < 2 * config.time_to_send())
			return false;
		int next_hop = config.routing_table()[config.destination_id()][0];
		if(next_hop == -1)
			return true;
		String data_msg = String.format("%d %d DATA %d %d %s",
				next_hop, config.node_id(),
				config.node_id(), config.destination_id(), config.message());
		write_to_file(fr_file, data_msg);
		config.set_is_message_sent(true);
		return false;
	}

	private void process_incoming_messages() {
		String str;
		try {
			while((str = to_file.readLine()) != null) {
				String words[] = str.split(" ");
				switch (words[2]) {
				case "DATA":
					process_data_message(str); break;
				case "HELLO":
					process_hello_message(str); calculate_MPR(); break;
				case "TC":
					if(process_tc_message(str)) calculate_routing_table();  break;
				default:
					process_odd_message(str); break;
				}
			}
		} catch (IOException e) {
			System.out.println(e + " in Thread_Main process_incoming_messages().");
		}
	}

	private void calculate_MPR() {
		// make all mprs none as we are going to choose them again.
		for(int i = 0 ; i < config.number_of_nodes() ; i++)
			if(config.one_hop_neighbors()[i] == State_of_Link.MPR)
				config.one_hop_neighbors()[i] = State_of_Link.Bidirectional;

		// coverage list to find which nodes are selected so far - initiate all to false (not selected)
        boolean coverage[] = new boolean[config.number_of_nodes()];
		
		for(int j = 0 ; j < config.number_of_nodes() ; j++) {
			State_of_Link cur_state = config.one_hop_neighbors()[j];
			if(cur_state == State_of_Link.Bidirectional || j == config.node_id())
				coverage[j] = true;
		}

        while(true) {
        	// calculate which one has the greatest coverage from not-selected ones
        	int two_hop_reachable[] = new int[config.number_of_nodes()];
        	for(int i = 0 ; i < config.number_of_nodes() ; i++) {
        		if(config.one_hop_neighbors()[i] == State_of_Link.Bidirectional)
        			for(int j = 0 ; j < config.number_of_nodes() ; j++)
        				if(config.two_hop_neighbors()[j][i] && !coverage[j])
        					two_hop_reachable[i]++;
        	}
        	// find the maximum coverage among all neighbors
        	int max_reachable = 0;
			int max_reach_node = -1;
			for (int i = 0 ; i < two_hop_reachable.length; i++) {
				if (i != config.node_id() && two_hop_reachable[i] > max_reachable) {
					max_reachable = two_hop_reachable[i];
					max_reach_node = i;
				}
			}
			// if no node has any link to an unselected two hop neighbor then, it is finished
			if(max_reach_node > -1 && max_reachable > 0) {
				config.one_hop_neighbors()[max_reach_node] = State_of_Link.MPR;
				for(int j = 0 ; j < config.number_of_nodes() ; j++)
					if(config.two_hop_neighbors()[j][max_reach_node] && !coverage[j])
						coverage[j] = true;
			}
			else
				break;
        }
	}

	private void process_data_message(String str) {
		try {
			String words[] = str.split(" ");
			int dst = Integer.parseInt(words[4]);
			if(dst == config.node_id())
				write_to_file(rc_file, str);
			else
				forward_data(dst, str);
		} catch (Exception e) {
			System.out.println(e + " in process_hello_message().");
		}
	}

	private void forward_data(int dst, String str) {
		if(config.routing_table()[dst][0] == -1) {
			return;
		}
		int data_index = str.indexOf("DATA");
		String msg = config.routing_table()[dst][0] + " " + config.node_id() + " " + str.substring(data_index);
		write_to_file(fr_file, msg);
	}

	private void process_hello_message(String str) {
		String words[] = str.split(" ");
		int sender_id = Integer.parseInt(words[1]);
		State_of_Link cur_state = config.one_hop_neighbors()[sender_id];
		
		// refresh timer
		config.neighbors_ttl()[sender_id] = 0; 
		
		// if he was not neighbor, now there is a unidirectional link
		if(cur_state == State_of_Link.None)
			config.one_hop_neighbors()[sender_id] = State_of_Link.Unidirectional;
		
		// make all of nodes reachable by the sender before unreachable
		for(int j = 0 ; j < config.two_hop_neighbors().length ; j++)
			config.two_hop_neighbors()[j][sender_id] = false;
		
		// index on the words of the line
		int index;
		for(index = 4 ; words[index].compareTo("BIDIR") != 0 ; index++) {
			int node_id = Integer.parseInt(words[index]);
			// if sender thinks there is a unidirectional link I know it is actually bidirectional 
			if(node_id == config.node_id())
				config.one_hop_neighbors()[sender_id] = State_of_Link.Bidirectional;
		}
		
		// list of nodes the sender has bidirectional link
		Vector<Integer> list_of_neighbors_of_src = new Vector<Integer>();
		for(index++ ; words[index].compareTo("MPR") != 0 ; index++) {
			int node_id = Integer.parseInt(words[index]);
			// If he thinks there is a bidirectional link, there actually is
			if(node_id == config.node_id())
				config.one_hop_neighbors()[sender_id] = State_of_Link.Bidirectional;
			else
				list_of_neighbors_of_src.add(node_id);
		}
		
		// if I have a bidirectional link then add the nodes reachable by sender to my two hop neighbors
		cur_state = config.one_hop_neighbors()[sender_id];
		if(cur_state == State_of_Link.Bidirectional || cur_state == State_of_Link.MPR)
			for(int i = 0 ; i < list_of_neighbors_of_src.size() ; i++)
				config.two_hop_neighbors()[list_of_neighbors_of_src.get(i)][sender_id] = true;
		
		// If sender chose me as an MPR then it is my MS
		for(index++ ; index < words.length ; index++) {
			int node_id = Integer.parseInt(words[index]);
			if(node_id == config.node_id())
				config.ms_nodes()[sender_id] = true;
			if(cur_state == State_of_Link.Bidirectional || cur_state == State_of_Link.MPR)
				config.two_hop_neighbors()[node_id][sender_id] = true;
		}
	}

	private boolean process_tc_message(String str) {
		String words[] = str.split(" ");
		int fromnbr = Integer.parseInt(words[1]);
		int srcnode = Integer.parseInt(words[3]);
		int new_seqno = Integer.parseInt(words[4]);
		int old_seqno = config.topology_seq_ttl()[srcnode].MS_seq_number();
		if(new_seqno < old_seqno)
			return false;
		config.topology_seq_ttl()[srcnode] = new TC_Entry(new_seqno, 0);
		if(new_seqno == old_seqno)
			return false;
		if(srcnode == config.node_id())
			return false;
		for(int i = 0 ; i < config.number_of_nodes() ; i++)
			config.topology_table()[srcnode][i] = false;
		for(int i = 6 ; i < words.length ; i++) {
			int msnode = Integer.parseInt(words[i]);
			config.topology_table()[srcnode][msnode] = true;
		}
		forward_tc(fromnbr, words);
		return true;
	}

	private void forward_tc(int fromnbr, String[] words) {
		if(!config.ms_nodes()[fromnbr])
			return;
		String tc_msg = "* " + config.node_id();
		for(int i = 2 ; i < words.length ; i++)
			tc_msg += (" " + words[i]);
		write_to_file(fr_file, tc_msg);
	}

	private void process_odd_message(String str) {
		System.out.println("!!!!!!!!!!");
		System.out.printf("ODD MESSAGE IN TOx.TXT FILE: %s\n", str);
		System.out.println("!!!!!!!!!!");
	}

	private void close_files() {
		try {
			fr_file.close();
			to_file.close();
			rc_file.close();
		} catch (Exception e) {
			System.out.println(e + " in Main_Thread close_files().");
		}
	}

	private boolean open_files() {
		try {
			String fr_file_name = String.format("from%d.txt", config.node_id());
			String to_file_name = String.format("to%d.txt", config.node_id());
			String rc_file_name = String.format("%dreceived.txt", config.node_id());
			File fr_f = new File(fr_file_name);
			File to_f = new File(to_file_name);
			File rc_f = new File(rc_file_name);
			if(!fr_f.exists()) {
				fr_f.createNewFile();
			}
			if(!to_f.exists()) {
				to_f.createNewFile();
			}
			if(!rc_f.exists()) {
				rc_f.createNewFile();
			}
			fr_file = new BufferedWriter(new FileWriter(fr_file_name, true));
			to_file = new BufferedReader(new FileReader(to_file_name));
			rc_file = new BufferedWriter(new FileWriter(rc_file_name, true));
			return true;
		} catch (Exception e) {
			System.out.println(e + " in open_files().");
			return false;
		}
	}
	
	private void write_to_file(BufferedWriter writer, String message)
	{
		try {
			writer.write(message);
			writer.newLine();
			writer.flush();
		}
		catch(Exception e) {
			System.out.println(e + " in write_to_file().");
		}
	}

	private void try_sleep(int mili_time) {
		try {
			sleep(mili_time);
		} catch (InterruptedException e) {
			System.out.println(e + " in try_sleep().");
		}
	}
}
