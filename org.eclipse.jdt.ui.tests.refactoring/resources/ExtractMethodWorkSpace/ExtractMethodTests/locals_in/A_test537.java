package locals_in;

public class A_test537 {
	public void foo() {
		final int i= 10;
		
		/*[*/Runnable run= new Runnable() {
			public void run() {
				System.out.println("" + i);
			}
		};/*]*/
		
		run.run();
	}
}
