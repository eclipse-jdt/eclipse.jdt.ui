package cast_out;

class Base {
	public void foo(int i) {
	}
}

class Derived extends Base {
	private void foo(char c) {
	}
}

public class TestHierarchyOverloadedPrivate {
	public int goo() {
		return 'a';
	}
	public void main(Derived d) {
		d.foo((int) 'a');
	}
}
