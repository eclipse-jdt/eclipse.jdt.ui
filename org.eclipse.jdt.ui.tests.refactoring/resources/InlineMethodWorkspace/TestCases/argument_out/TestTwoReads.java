package argument_out;

public class TestTwoReads {
	public void main() {
		int x = value();
		int i= x;
		int y= x;
	}
	
	public void foo(int x) {
		int i= x;
		int y= x;
	}
	
	public int value() {
		return 10;
	}
}
