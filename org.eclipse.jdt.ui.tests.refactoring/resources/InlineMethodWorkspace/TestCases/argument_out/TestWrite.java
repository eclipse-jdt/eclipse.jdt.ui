package argument_out;

public class TestWrite {
	public void main() {
		int x = value();
		x= 10;
	}
	
	public void foo(int x) {
		x= 10;
	}
	
	public int value() {
		return 10;
	}
}
