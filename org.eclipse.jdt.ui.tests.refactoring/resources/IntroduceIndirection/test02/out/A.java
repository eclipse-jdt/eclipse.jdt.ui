package p;

public class A extends B {
	
	// Test warning because of super keyword

	/**
	 * @param b
	 */
	public static void bar(B b) {
		b.foo();
	}

	{
		super.foo(); //<------invoke here
	}
}
