package locals_out;
public class A_test510 {
	public void foo() {
		/*]*/int i= extracted();/*[*/
		
		i++;
	}
	protected int extracted() {
		foo();
		int i= 10;
		i= 2;
		return i;
	}
}
