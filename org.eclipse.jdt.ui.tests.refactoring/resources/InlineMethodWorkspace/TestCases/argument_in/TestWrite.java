package argument_in;

public class TestWrite {
	public void main() {
		/*]*/foo(value());/*[*/
	}
	
	public void foo(int x) {
		x= 10;
	}
	
	public int value() {
		return 10;
	}
}
