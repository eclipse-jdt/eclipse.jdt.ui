package p;

public abstract class A {

	abstract void foo();

	/**
	 * @deprecated Use {@link #foo()} instead
	 */
	abstract void foo();
}
