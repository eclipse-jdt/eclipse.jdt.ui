package destination_in;

public class A_test1051 {
	public void foo() {
		Runnable r= new Runnable() {
			public void run() {
				/*[*/bar();/*]*/
			}
		};
	}
	public void bar() {
	}
}
