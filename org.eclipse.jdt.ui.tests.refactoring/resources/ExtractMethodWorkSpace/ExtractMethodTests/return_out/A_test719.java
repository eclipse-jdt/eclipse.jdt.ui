package return_out;
public class A_test719 {
	public boolean foo() {
		return extracted();
	}

	protected boolean extracted() {
		/*[*/if (foo())
			return true;
		if (foo())
			return true;
		else
			return false;/*]*/
	}
}

