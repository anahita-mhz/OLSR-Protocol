import java.util.Arrays;

public class Node {
	public static void main(String[] args) {
		Config config = read_config(args);
		if(config == null)
			System.exit(-1);
		Main_Thread main_thread = new Main_Thread(config);
		main_thread.start();
	}

	private static Config read_config(String[] args) {
		Config config = new Config();
		config.set_number_of_nodes(10);
		try {
			config.set_node_id(Integer.parseInt(args[0]));
			config.set_destination_id(Integer.parseInt(args[1]));
			String message = "";
			if(config.destination_id() == config.node_id())
				config.set_is_message_sent(true);
			else {
				for(int i = 2 ; i < args.length - 1 ; i++)
					message += args[i];
				//message = message.substring(1, message.length()-1);
				config.set_message(message);
				config.set_time_to_send(Integer.parseInt(args[args.length - 1]));
			}
			config.set_routing_table(new int[config.number_of_nodes()][2]);
			for(int i = 0 ; i < config.number_of_nodes() ; i++) {
				config.routing_table()[i][0] = -1;
				config.routing_table()[i][1] = Integer.MAX_VALUE;
			}
			config.set_topology_table(new boolean[config.number_of_nodes()][config.number_of_nodes()]);
			config.set_topology_seq_ttl(new TC_Entry[config.number_of_nodes()]);
			Arrays.fill(config.topology_seq_ttl(), new TC_Entry(0, 0));
			config.set_one_hop_neighbors(new State_of_Link[config.number_of_nodes()]);
			Arrays.fill(config.one_hop_neighbors(), State_of_Link.None);
			config.set_neighbors_ttl(new int[config.number_of_nodes()]);
			config.set_two_hop_neighbors(new boolean[config.number_of_nodes()][config.number_of_nodes()]);
			config.set_ms_nodes(new boolean[config.number_of_nodes()]);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e + " in read_config().");
			config = null;
		}
		return config;
	}
}
