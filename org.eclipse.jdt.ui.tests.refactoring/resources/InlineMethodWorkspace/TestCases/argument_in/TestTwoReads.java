package argument_in;

public class TestTwoReads {
	public void main() {
		/*]*/foo(value());/*[*/
	}
	
	public void foo(int x) {
		int i= x;
		int y= x;
	}
	
	public int value() {
		return 10;
	}
}
