package cast_out;

public class TestNotOverloaded {
	public void foo(int i) {
	}
	public int goo() {
		return 'a';
	}
	public void main() {
		foo('a');
	}
}
