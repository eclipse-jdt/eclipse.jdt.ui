package cast_in;

class Base2 {
	public void foo(int i) {
	}
}

class Derived2 extends Base2 {
	public void foo(char c) {
	}
}

public class TestHierarchyOverloadedPrimitives {
	public int goo() {
		return 'a';
	}
	public void main(Derived2 d) {
		d.foo(/*]*/goo()/*[*/);
	}
}
