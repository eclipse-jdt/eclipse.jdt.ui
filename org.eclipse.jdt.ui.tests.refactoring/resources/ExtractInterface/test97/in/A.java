package p;

public class A {
	public int foo() {
		return 0;
	}
}

class Tester {
	void bar() {
		A t= null;
		int i= t.foo();
		t.hashCode();
	}
}
