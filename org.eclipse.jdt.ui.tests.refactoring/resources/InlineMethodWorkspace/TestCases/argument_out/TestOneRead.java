package argument_out;

public class TestOneRead {
	public void main() {
		int i= value();
	}
	
	public void foo(int x) {
		int i= x;
	}
	
	public int value() {
		return 10;
	}
}
