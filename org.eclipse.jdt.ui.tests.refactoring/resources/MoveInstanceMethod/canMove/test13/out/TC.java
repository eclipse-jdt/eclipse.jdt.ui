package p1;
public class TC {
	public void runBare() {
	}

	/**
	 * Run the test case
	 * @param tr TODO
	 */
	protected void run(TR tr) {
		tr.startTest(this);
		P p= new P() {
			public void protect() throws Throwable {
				runBare();
			}
		};
		tr.runProtected(this, p);
			// Some comment
		tr.endTest(this);
	}
}