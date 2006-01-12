package p;

public class Foo {
	
	A a;
	B b;
	C c;
	
	{
		a.foo();
		b.foo();	// <--- invoke here
		c.foo();	
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
