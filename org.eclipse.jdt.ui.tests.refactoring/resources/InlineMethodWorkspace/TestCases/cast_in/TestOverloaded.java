package cast_in;

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
		foo(/*]*/goo()/*[*/);
	}
}
