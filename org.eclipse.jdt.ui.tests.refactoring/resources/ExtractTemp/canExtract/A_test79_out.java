package p;

class A {
	String fName;
	/**
	 * Override to run the test and assert its state.
	 * @exception Throwable if any exception is thrown
	 */
	protected void runTest() throws Throwable {
		String strong= fName+"\" not found";
		System.out.println("Method \"" + strong);
	}
}
