package p;

public class A {

	/**
	 * @deprecated Use {@link #foo()} instead
	 */
	@Some
	@Thing
	@Else
	void foo() {
		foo();
	}

	@Some
	@Thing
	@Else
	void foo() {
		
	}
}
