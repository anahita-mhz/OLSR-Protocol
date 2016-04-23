
public class TC_Entry {
	private int MS_seq_number;
	private int holding_time;
	
	public TC_Entry(int MS_seq_number, int holding_time) {
		super();
		this.MS_seq_number = MS_seq_number;
		this.holding_time = holding_time;
	}
	public int MS_seq_number() {
		return MS_seq_number;
	}
	public void set_MS_seq_number(int mS_seq_number) {
		MS_seq_number = mS_seq_number;
	}
	public int holding_time() {
		return holding_time;
	}
	public void set_holding_time(int holding_time) {
		this.holding_time = holding_time;
	}
}
