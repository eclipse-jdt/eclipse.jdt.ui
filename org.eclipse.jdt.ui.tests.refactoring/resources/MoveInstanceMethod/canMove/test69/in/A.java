package p;

import p2.B;

public class A {
	public B b = new B();

	public static void staticMethod() {
	}

	public void moveMe() {
		b.m();
		staticMethod();
	}
}
