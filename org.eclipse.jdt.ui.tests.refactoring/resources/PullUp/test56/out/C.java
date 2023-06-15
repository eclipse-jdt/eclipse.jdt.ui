package p;

public class C extends A {
	
	public void foo() {
		B b = new B();
		A a = new A();
		a.a(b);
		b.m(b);
	}

}