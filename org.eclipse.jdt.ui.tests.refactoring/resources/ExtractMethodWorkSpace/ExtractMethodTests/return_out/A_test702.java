package return_out;

public class A_test702 {
	public boolean foo() {
		return extracted();
	}

	protected boolean extracted() {
		/*[*/if (10 == 10)
			return true;
		else
			return false;/*]*/
	}
}
