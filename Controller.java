import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Controller {
	
	private static BufferedWriter writers[];
	private static BufferedReader readers[];
	private static boolean topology[][];
	
	public static void main(String[] args) {
		try {
			init();
			Scanner sc = new Scanner(new File("topology.txt"));
			String line = null;
			int topology_time = 0;
			for(int i = 0 ; i < 120 ; i++) {
				while(i >= topology_time && topology_time >= 0) {
					change_topology(line);
					if(sc.hasNextLine()) {
						line = sc.nextLine();
						String[] words = line.split(" ");
						topology_time = Integer.parseInt(words[0]);
						if(topology_time < 0)
							System.out.println("A topology inconsistancy. There exists a negative time!");
					} else
						topology_time = -1;
				}
				transfer_messages();
				try_sleep(1000);
			}
			terminate();
			sc.close();
		} catch (Exception e) {
			System.out.println(e + " in main().");
		}
	}
	
	private static void terminate() {
		for(int i = 0 ; i < 10 ; i++) {
			try {
				if(writers[i] != null)
					writers[i].close();
				if(readers[i] != null)
					readers[i].close();
			} catch (Exception e) {
				System.out.println(e + " in terminate().");
			}
		}
	}

	private static void init() {
		try_sleep(200);
		writers = new BufferedWriter[10];
		readers = new BufferedReader[10];
		topology = new boolean[10][10];
		
		for(int i = 0 ; i < 10 ; i++) {
			String writer_file_name = String.format("to%d.txt", i);
			String reader_file_name = String.format("from%d.txt", i);
			try {
				if((new File(writer_file_name)).exists()) {
					writers[i] = new BufferedWriter(new FileWriter(writer_file_name, true));
				}
				if((new File(reader_file_name)).exists()) {
					readers[i] = new BufferedReader(new FileReader(reader_file_name));
				}
			} catch (Exception e) {
				System.out.println(e + " in init() with node id = " + i + ".");
			}
		}
	}

	private static void transfer_messages() {
		for(int i = 0 ; i < 10 ; i++) {
			if(readers[i] == null)
				try_make_reader(i);
			if(readers[i] != null) {
				String str;
				try {
					while((str = readers[i].readLine()) != null)
						send_message(str, i);
				} catch (IOException e) {
					System.out.println(e + " in transfer_messages() with node id = " + i + ".");
				}
			}
		}
	}

	private static void try_make_reader(int i) {
		String reader_file_name = String.format("from%d.txt", i);
		if((new File(reader_file_name)).exists()) {
			try {
				readers[i] = new BufferedReader(new FileReader(reader_file_name));
			} catch (FileNotFoundException e) {
				System.out.println(e + " in make_reader().");
			}
		}
	}

	private static void send_message(String str, int src_id) {
		String[] words = str.split(" ");
		if(words[0].compareTo("*") == 0) {
			broadcast_msg(str, src_id);
		} else {
			try {
				int des_id = Integer.parseInt(words[0]);
				unicast_msg(str, src_id, des_id);
			} catch (Exception e) {
				System.out.println(e + " in send_message() with node id = " + src_id + ".");
			}
		}
	}

	private static void unicast_msg(String str, int src_id, int des_id) {
		if(!topology[src_id][des_id])
			return;
		try {
			if(writers[des_id] == null) {
				String writer_file_name = String.format("to%d.txt", des_id);
				writers[des_id] = new BufferedWriter(new FileWriter(writer_file_name, true));
			}
			writers[des_id].write(str);
			writers[des_id].newLine();
			writers[des_id].flush();
		} catch (IOException e) {
			System.out.println(e + " in unicast_msg() with node id = " + des_id + ".");
		}
	}

	private static void broadcast_msg(String str, int src_id) {
		for(int i = 0 ; i < 10 ; i++) {
			if(i == src_id)
				continue;
			unicast_msg(str, src_id, i);
		}
	}

	private static void change_topology(String line) {
		if(line == null)
			return;
		try {
			String[] words = line.split(" ");
			boolean is_up = (words[1].toLowerCase().compareTo("up") == 0 ? true : false);
			int src_id = Integer.parseInt(words[2]);
			int des_id = Integer.parseInt(words[3]);
			topology[src_id][des_id] = is_up;
		} catch (Exception e) {
			System.out.println(e + " in change_topology().");
		}
	}

	private static void try_sleep(int mili_time) {
		try {
			Thread.sleep(mili_time);
		} catch (InterruptedException e) {
			System.out.println(e + " in try_sleep().");
		}
	}
}
