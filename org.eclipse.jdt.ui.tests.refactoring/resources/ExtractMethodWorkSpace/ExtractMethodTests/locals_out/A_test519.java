package locals_out;

public class A_test519 {
	public void foo() {
		int i;

		extracted();
		
		i= 20;		
	}

	protected void extracted() {
		int i;
		/*[*/i= 10;/*]*/
	}
}