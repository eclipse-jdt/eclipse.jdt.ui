package cast_in;

public class TestOverloadedPrimitives {
	public void foo(int i) {
	}
	public void foo(char c) {
	}
	public int goo() {
		return 'a';
	}
	public void main() {
		foo(/*]*/goo()/*[*/);
	}
}
