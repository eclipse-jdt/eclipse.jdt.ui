package p;

interface B {
	public abstract int foo();
}

public class A implements B {
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