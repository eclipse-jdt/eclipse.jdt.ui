package cast_in;

class Base {
	public void foo(int i) {
	}
}

class Derived extends Base {
	public void foo(char c) {
	}
}

public class TestHierarchyOverloadedPrimitives {
	public int goo() {
		return 'a';
	}
	public void main(Derived d) {
		d.foo(/*]*/goo()/*[*/);
	}
}
