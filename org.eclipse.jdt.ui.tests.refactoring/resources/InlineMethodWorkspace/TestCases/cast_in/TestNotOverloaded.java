package cast_in;

public class TestNotOverloaded {
	public void foo(int i) {
	}
	public int goo() {
		return 'a';
	}
	public void main() {
		foo(/*]*/goo()/*[*/);
	}
}
