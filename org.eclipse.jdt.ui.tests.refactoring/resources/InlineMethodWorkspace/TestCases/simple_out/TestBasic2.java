package simple_out;

public class TestBasic2 {
	public void main() {
		for (int i= 0; i < 10; i++) {
			baz();
		}
		baz();
	}
	
	public void bar() {
		for (int i= 0; i < 10; i++) {
			baz();
		}
		baz();
	}
	public void baz() {
	}
}
