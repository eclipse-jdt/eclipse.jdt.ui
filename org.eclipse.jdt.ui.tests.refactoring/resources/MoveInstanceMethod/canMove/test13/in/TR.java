package p1;

public class TR {

	/**
	 * Run the test case
	 * @param test
	 */
	protected void run(final TC test) {
		startTest(test);
		P p= new P() {
			public void protect() throws Throwable {
				test.runBare();
			}
		};
		runProtected(test, p);
			// Some comment
		endTest(test);
	}

	private void runProtected(TC test, P p) {
	}

	private void endTest(TC test) {
	}

	private void startTest(TC test) {
	}

}