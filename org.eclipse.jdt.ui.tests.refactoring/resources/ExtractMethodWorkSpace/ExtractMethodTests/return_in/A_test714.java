package return_in;

public class A_test714 {
	public boolean foo() {
		/*[*/boolean b= false;
		foo();
		return (b == true);/*]*/
	}
}
