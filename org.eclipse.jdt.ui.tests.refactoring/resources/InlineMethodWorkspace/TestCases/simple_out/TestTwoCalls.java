package simple_out;

public class TestTwoCalls {
	public void main() {
		int i = 10;
		i= 10;
		bar();
		bar();
		baz();
		int i1 = 10;
		i1= 10;
		bar();
		bar();
	}
	void baz() {
	}
	void bar() {
	}
}
