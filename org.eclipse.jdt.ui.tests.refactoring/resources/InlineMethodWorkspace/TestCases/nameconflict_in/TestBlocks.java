package nameconflict_in;

public class TestBlocks {
	public void main() {
		if (true) {
			/*]*/foo();/*[*/
		}
		if (true) {
			int x= 1;
		}
	}
	
	private void foo() {
		int x= 1;
	}
}
