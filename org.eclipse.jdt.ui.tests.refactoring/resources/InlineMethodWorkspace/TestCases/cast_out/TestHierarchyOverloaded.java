package cast_out;

class Woo {
}

class Zoo extends Woo {
}

class Base {
	public void foo(Woo w) {
	}
}

class Derived extends Base {
	public void foo(Zoo z) {
	}
}

public class TestHierarchyOverloaded {
	public Woo goo() {
		return new Zoo();
	}
	public void main(Derived d) {
		d.foo((Woo) new Zoo());
	}
}
