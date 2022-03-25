package p;

public class A {
	static class B {
		static int f= 22;
		static A B= new A();
	}

	static int f= 42;

	static int foo() {
		int r= A.B.f;
		B A= new B();
		A B= new A();
		int p;
		return r;
	}
}