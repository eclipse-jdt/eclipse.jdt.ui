package locals_out;

public class A_test515 {
	public void foo() {
		int y= 10;
		
		int x = extracted(y);
		
		x++;
	}

	protected int extracted(int y) {
		/*[*/int x= y;/*]*/
		return x;
	}
}
