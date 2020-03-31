package p;

public class A {

	public Object o;

	public void bar() {
		this.o= null;
	}
}

class B extends A {

	public void foo() {
		// comment 1
		this.o= null; /* comment 2 */
		this.o= /* comment 3*/ null;
		this.bar();
		// comment 4
	}
}
