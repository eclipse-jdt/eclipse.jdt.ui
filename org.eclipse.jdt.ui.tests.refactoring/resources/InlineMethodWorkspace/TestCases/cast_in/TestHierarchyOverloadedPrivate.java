package cast_in;

class Base3 {
	public void foo(int i) {
	}
}

class Derived3 extends Base3 {
	private void foo(char c) {
	}
}

public class TestHierarchyOverloadedPrivate {
	public int goo() {
		return 'a';
	}
	public void main(Derived3 d) {
		d.foo(/*]*/goo()/*[*/);
	}
}
