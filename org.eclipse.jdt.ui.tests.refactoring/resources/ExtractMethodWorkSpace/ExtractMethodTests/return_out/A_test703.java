package return_out;

public class A_test703 {
	public boolean foo() {
		return extracted();
	}

	protected boolean extracted() {
		/*[*/if (0 == 0) {
			if (10 == 10)
				return true;
			else
				return false;
		}
		foo();
		return false;/*]*/
	}
}
