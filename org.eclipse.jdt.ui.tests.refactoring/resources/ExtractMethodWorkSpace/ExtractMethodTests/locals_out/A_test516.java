package locals_out;

public class A_test516 {
	public void foo() {
		int j = extracted();
		
		j++;
	}

	protected int extracted() {
		/*[*/int i= 10, j= 20;/*]*/
		return j;
	}
}