package p1;

import static java.lang.Math.PI;

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
				double d= PI;
			}
		};
		tr.runProtected(this, p);
	
		tr.endTest(this);
	}
}