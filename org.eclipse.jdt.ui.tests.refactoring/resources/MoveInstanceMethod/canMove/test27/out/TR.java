package p1;
public class TR {

	/**
	 * Runs the test
	 * @param test the test to run
	 * @deprecated Use {@link p1.TC#run(p1.TR)} instead
	 */
	protected void run(final TC test) {
		test.run(this);
	}

	void handleRun(TC test) {
	}

	void runProtected(TC test, P p) {
	}

	void endTest(TC test) {
	}

	void startTest(TC test) {
	}

}