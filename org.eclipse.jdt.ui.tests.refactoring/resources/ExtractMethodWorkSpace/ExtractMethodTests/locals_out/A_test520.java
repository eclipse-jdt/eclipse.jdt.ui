package locals_out;

public class A_test520 {
	public void foo() {
		int i= 0;

		extracted(i);
		
		i= 20;		
	}

	protected void extracted(int i) {
		/*[*/int y= i;/*]*/
	}
}