package locals_out;

public class A_test512 {
	public void foo() {
		int x = extracted();
		
		x++;
	}

	protected int extracted() {
		/*[*/int x= 0;
		int y= x;/*]*/
		return x;
	}
}
