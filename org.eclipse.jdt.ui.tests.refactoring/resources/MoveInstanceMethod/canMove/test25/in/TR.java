package p1;

public class TR {
	protected void run(final TC test) {
		startTest(test);
		P p= new P() {
			public void protect() throws Throwable {
				test.runBare();
				handleRun(test);
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