package cast_out;

class Woo1 {
}

class Zoo1 extends Woo1 {
}

class Base1 {
	public void foo(Woo1 w) {
	}
}

class Derived1 extends Base1 {
	public void foo(Zoo1 z) {
	}
}

public class TestHierarchyOverloaded {
	public Woo1 goo() {
		return new Zoo1();
	}
	public void main(Derived1 d) {
		d.foo((Woo1) new Zoo1());
	}
}
