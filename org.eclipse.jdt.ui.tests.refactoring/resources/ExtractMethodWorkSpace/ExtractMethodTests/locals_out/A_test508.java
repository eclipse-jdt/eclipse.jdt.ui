package locals_out;

public class A_test508 {
	public void foo() {
		int x= 0;
		int y= 0;
		
		x = extracted();
		
		y= x;
	}

	protected int extracted() {
		int x;
		int y;
		/*[*/x= 10;
		y= 20;/*]*/
		return x;
	}	
}