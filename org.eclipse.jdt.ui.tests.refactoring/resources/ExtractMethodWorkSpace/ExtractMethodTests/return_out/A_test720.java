package return_out;

public class A_test720 {
	private boolean flag;
	public boolean foo() {
		if (flag) {
			return extracted();
		}
		return true;
	}
	protected boolean extracted() {
		/*[*/try {
			foo();
		} catch(Exception e) {
		}
		return false;/*]*/
	}
}

