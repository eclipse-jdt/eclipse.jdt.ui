package p;

public class A implements I {
	public int foo() {
		return 0;
	}
}

class Tester {
	void bar() {
		I t= null;
		int i= t.foo();
		t.hashCode();
	}
}
