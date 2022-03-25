package p;

public class A {
	static class B {
		static int f = 22;
		static A B = new A();
	}

	static int f = 42;

	static int foo() {
		B A = new B();
		A B = new A();
		return p.A.B.f;
	}
}