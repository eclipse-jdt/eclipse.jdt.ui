//5, 32, 5, 32
package p;

class A {
	private static final int CONST = 9;

	/*
	 * (non-Javadoc) comment
	 */
	public void foo() {
		final int lineNumber2= /*preserve*/CONST/*this*/;
	}
}
