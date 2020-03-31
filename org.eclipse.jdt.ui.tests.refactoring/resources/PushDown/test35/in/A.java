package p;

public class A {

	public Object o;

	public void foo() {
		// comment 1
		A.this.o= null; /* comment 2 */
		p.A.this.o= /* comment 3*/ null;
		this.bar();
		// comment 4
	}

	public void bar() {
		this.o= null;
	}
}

class B extends A {
}
