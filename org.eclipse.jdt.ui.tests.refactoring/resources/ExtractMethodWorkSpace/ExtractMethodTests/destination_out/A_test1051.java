package destination_out;

public class A_test1051 {
	public void foo() {
		Runnable r= new Runnable() {
			public void run() {
				extracted();
			}
		};
	}
	public void bar() {
	}
	protected void extracted() {
		/*[*/bar();/*]*/
	}
}
