package p1;
public class TC {
	public void runBare() {
	}

	/**
	 * @param tr
	 */
	protected void run(TR tr) {
		tr.startTest(this);
		P p= new P() {
			public void protect() throws Throwable {
				runBare();
			}
		};
		tr.runProtected(this, p);
	
		tr.endTest(this);
	}
}