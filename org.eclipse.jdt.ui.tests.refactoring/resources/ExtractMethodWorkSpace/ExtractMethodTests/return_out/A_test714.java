package return_out;

public class A_test714 {
	public boolean foo() {
		return extracted();
	}

	protected boolean extracted() {
		/*[*/boolean b= false;
		foo();
		return (b == true);/*]*/
	}
}
