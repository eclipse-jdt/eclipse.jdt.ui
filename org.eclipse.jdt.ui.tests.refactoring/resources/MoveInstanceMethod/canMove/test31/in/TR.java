package p1;
import java.util.List;
import static java.lang.Math.cos;
public enum TR implements P {
	PASSED, FAILED;
	/**
	 * Runs the test
	 * @param test the test to run
	 */
	protected void run(final TC test) {
		List<Integer> integers= null;
		startTest(test);
		P p= new P() {
			public void protect() throws Throwable {
				test.runBare();
				handleRun(test);
				double d= cos(0);
			}
		};
		runProtected(test, p);

		endTest(test);
	}

	private void handleRun(TC test) {
	}

	private void runProtected(TC test, P p) {
	}

	private void endTest(TC test) {
	}

	private void startTest(TC test) {
	}

}