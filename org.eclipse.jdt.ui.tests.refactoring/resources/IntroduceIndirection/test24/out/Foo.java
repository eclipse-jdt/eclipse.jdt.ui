package p;

public class Foo {
	
	/* (non-Javadoc)
	 * @see p.A#foo()
	 */
	public static void bar(A target) {
		target.foo();
	}

	A a;
	B b;
	C c;
	
	{
		Foo.bar(a);
		Foo.bar(b);	// <--- invoke here
		Foo.bar(c);	
	}

}

class A {
	void foo() {}
}
class B extends A {
	void foo() {}
}
class C extends A {
	void foo() {}
}
