package locals_out;

public class A_test537 {
	public void foo() {
		final int i= 10;
		
		Runnable run = extracted(i);
		
		run.run();
	}

	protected Runnable extracted(final int i) {
		/*[*/Runnable run= new Runnable() {
			public void run() {
				System.out.println("" + i);
			}
		};/*]*/
		return run;
	}
}
