package p;

class A {
	String fName;
	/**
	 * Override to run the test and assert its state.
	 * @exception Throwable if any exception is thrown
	 */
	protected void runTest() throws Throwable {
		System.out.println("Method \""+fName+"\" not found");
	}
}
