package locals_out;

public class A_test507 {
	public void foo() {
		int x= 0;
		
		x = extracted();
		
		int y= x;
	}

	protected int extracted() {
		int x;
		/*[*/x= 10;/*]*/
		return x;
	}	
}