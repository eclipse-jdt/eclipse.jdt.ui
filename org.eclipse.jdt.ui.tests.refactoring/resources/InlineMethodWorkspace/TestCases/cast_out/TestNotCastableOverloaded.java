package cast_out;

class Woo {
}

public class TestNotCastableOverloaded {
	public void foo(int i) {
	}
	public void foo(Woo w) {
	}
	public int goo() {
		return 'a';
	}
	public void main() {
		foo('a');
	}
}
