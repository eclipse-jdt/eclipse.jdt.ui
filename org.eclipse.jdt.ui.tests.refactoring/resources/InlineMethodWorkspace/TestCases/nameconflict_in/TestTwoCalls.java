package nameconflict_in;

public class TestTwoCalls {
	public void main() {
		int x= 0;
		/*]*/foo();/*[*/
		/*]*/foo();/*[*/
	}
	
	private void foo() {
		int x= 1;
	}
}
