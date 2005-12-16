package p;

public interface A {

	void foo();

	/**
	 * @deprecated Use {@link #foo()} instead
	 */
	void foo();
}
