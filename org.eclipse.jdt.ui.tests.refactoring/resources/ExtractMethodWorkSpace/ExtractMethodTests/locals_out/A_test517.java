package locals_out;

public class A_test517 {
	public void foo() {
		int i;
		int j = extracted();
		i= j + 10;
	}

	protected int extracted() {
		/*[*/int i;
		int j;
		j= 20;/*]*/
		return j;
	}
}