package return_out;

public class A_test716 {
	public boolean flag;
	public boolean foo() {
		return extracted();
	}
	protected boolean extracted() {
		/*[*/if (flag)
			return false;
		else
			throw new NullPointerException();/*]*/
	}
}
