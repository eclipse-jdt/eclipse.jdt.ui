package cast_out;

class Woo4 {
}

public class TestNotCastableOverloaded {
	public void foo(int i) {
	}
	public void foo(Woo4 w) {
	}
	public int goo() {
		return 'a';
	}
	public void main() {
		foo('a');
	}
}
