
public class Config {
	
	private int number_of_nodes;
	private int node_id;
	private int destination_id;
	private String message;
	private int time_to_send;
	private boolean is_message_sent;
	private int seqno;
	private int routing_table[][];
	private boolean topology_table[][];
	private TC_Entry topology_seq_ttl[];
	private State_of_Link one_hop_neighbors[];
	private int neighbors_ttl[];
	private boolean two_hop_neighbors[][];
	private boolean ms_nodes[];
	
	public int seqno() {
		return seqno;
	}
	public void set_seqno(int seqno) {
		this.seqno = seqno;
	}
	public boolean is_message_sent() {
		return is_message_sent;
	}
	public void set_is_message_sent(boolean is_message_sent) {
		this.is_message_sent = is_message_sent;
	}
	public TC_Entry[] topology_seq_ttl() {
		return topology_seq_ttl;
	}
	public void set_topology_seq_ttl(TC_Entry[] topology_seq_ttl) {
		this.topology_seq_ttl = topology_seq_ttl;
	}
	public int number_of_nodes() {
		return number_of_nodes;
	}
	public void set_number_of_nodes(int number_of_nodes) {
		this.number_of_nodes = number_of_nodes;
	}
	public boolean[] ms_nodes() {
		return ms_nodes;
	}
	public void set_ms_nodes(boolean[] ms_nodes) {
		this.ms_nodes = ms_nodes;
	}
	public int[] neighbors_ttl() {
		return neighbors_ttl;
	}
	public void set_neighbors_ttl(int[] neighbors_ttl) {
		this.neighbors_ttl = neighbors_ttl;
	}
	public int[][] routing_table() {
		return routing_table;
	}
	public void set_routing_table(int[][] routing_table) {
		this.routing_table = routing_table;
	}
	public boolean[][] topology_table() {
		return topology_table;
	}
	public void set_topology_table(boolean[][] tc_table) {
		this.topology_table = tc_table;
	}
	public State_of_Link[] one_hop_neighbors() {
		return one_hop_neighbors;
	}
	public void set_one_hop_neighbors(State_of_Link[] one_hop_neighbors) {
		this.one_hop_neighbors = one_hop_neighbors;
	}
	public boolean[][] two_hop_neighbors() {
		return two_hop_neighbors;
	}
	public void set_two_hop_neighbors(boolean[][] two_hop_neighbors) {
		this.two_hop_neighbors = two_hop_neighbors;
	}
	public int node_id() {
		return node_id;
	}
	public void set_node_id(int node_id) {
		this.node_id = node_id;
	}
	public int destination_id() {
		return destination_id;
	}
	public void set_destination_id(int destination_id) {
		this.destination_id = destination_id;
	}
	public String message() {
		return message;
	}
	public void set_message(String message) {
		this.message = message;
	}
	public int time_to_send() {
		return time_to_send;
	}
	public void set_time_to_send(int time_to_send) {
		this.time_to_send = time_to_send;
	}
}
