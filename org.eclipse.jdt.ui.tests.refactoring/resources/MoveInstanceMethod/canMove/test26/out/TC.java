package p1;

import java.util.List;

public class TC {
	public void runBare() {
	}

	/**
	 * Runs the test
	 * @param tr TODO
	 */
	protected void run(final TR tr) {
		List<Integer> integers= null;
		tr.startTest(this);
		P p= new P() {
			public void protect() throws Throwable {
				runBare();
				tr.handleRun(TC.this);
			}
		};
		tr.runProtected(this, p);
	
		tr.endTest(this);
	}
}