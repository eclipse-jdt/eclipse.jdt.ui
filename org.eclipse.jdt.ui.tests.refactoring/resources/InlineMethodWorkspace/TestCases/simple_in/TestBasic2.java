package simple_in;

public class TestBasic2 {
	public void main() {
		/*]*/bar();/*[*/
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
