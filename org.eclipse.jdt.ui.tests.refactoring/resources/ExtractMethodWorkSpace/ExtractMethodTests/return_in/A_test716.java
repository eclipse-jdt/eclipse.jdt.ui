package return_in;

public class A_test716 {
	public boolean flag;
	public boolean foo() {
		/*[*/if (flag)
			return false;
		else
			throw new NullPointerException();/*]*/
	}
}
