package locals_out;

public class A_test511 {
	public void foo() {
		int x = extracted();
		
		x++;
	}

	protected int extracted() {
		/*[*/int x= 0;/*]*/
		return x;
	}
}
