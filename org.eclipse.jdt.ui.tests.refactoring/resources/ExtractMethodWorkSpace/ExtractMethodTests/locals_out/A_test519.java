package locals_out;

public class A_test519 {
	public void foo() {
		/*]*/int j= extracted();/*[*/
		g(j);
	}
	protected int extracted() {
		int i= 10, j= 20;
		return j;
	}
	public void g(int i) {
	}
}