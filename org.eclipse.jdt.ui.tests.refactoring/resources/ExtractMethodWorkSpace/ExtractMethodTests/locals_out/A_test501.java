package locals_out;

public class A_test501 {
	public void foo() {
		int x= 10;
		
		extracted();
	}

	protected void extracted() {
		int x;
		/*[*/x= 20;
		int y= x;/*]*/
	}
}