package p1;
public class TC {
	public void runBare() {
	}

	protected void run(final TR tr) {
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