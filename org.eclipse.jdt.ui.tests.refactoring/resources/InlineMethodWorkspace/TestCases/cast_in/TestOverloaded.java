package cast_in;

class Base5 {
}

class Derived5 extends Base5 {
}

public class TestOverloaded {
	public void foo(Derived5 d) {
	}
	public void foo(Base5 b) {
	}
	public Base5 goo() {
		return new Derived5();
	}
	public void main() {
		foo(/*]*/goo()/*[*/);
	}
}
