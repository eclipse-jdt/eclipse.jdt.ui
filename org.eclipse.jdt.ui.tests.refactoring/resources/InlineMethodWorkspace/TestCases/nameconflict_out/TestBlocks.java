package nameconflict_out;

public class TestBlocks {
	public void main() {
		if (true) {
			int x= 1;
		}
		if (true) {
			int x= 1;
		}
	}
	
	private void foo() {
		int x= 1;
	}
}
