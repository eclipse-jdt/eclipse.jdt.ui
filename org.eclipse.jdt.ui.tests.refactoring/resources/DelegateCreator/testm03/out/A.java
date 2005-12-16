package p;

public class A {

	@Some
	@Thing
	@Else
	void foo() {
		
	}

	/**
	 * @deprecated Use {@link #foo()} instead
	 */
	@Some
	@Thing
	@Else
	void foo() {
		foo();
	}
}
