package simple_in;

public class TestTwoCalls {
	public void main() {
		toInline(10);
		baz();
		toInline(10);
	}
	void toInline(int i) {
		i= 10;
		bar();
		bar();
	}
	void baz() {
	}
	void bar() {
	}
}
