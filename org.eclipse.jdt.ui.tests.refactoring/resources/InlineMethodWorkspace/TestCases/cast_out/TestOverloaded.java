package cast_out;

class Base {
}

class Derived extends Base {
}

public class TestOverloaded {
	public void foo(Derived d) {
	}
	public void foo(Base b) {
	}
	public Base goo() {
		return new Derived();
	}
	public void main() {
		foo((Base) new Derived());
	}
}
