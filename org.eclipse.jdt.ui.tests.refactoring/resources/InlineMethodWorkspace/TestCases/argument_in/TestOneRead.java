package argument_in;

public class TestOneRead {
	public void main() {
		/*]*/foo(value());/*[*/
	}
	
	public void foo(int x) {
		int i= x;
	}
	
	public int value() {
		return 10;
	}
}
