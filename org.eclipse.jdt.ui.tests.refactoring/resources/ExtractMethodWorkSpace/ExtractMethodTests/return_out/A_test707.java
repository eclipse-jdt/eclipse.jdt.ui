package return_out;

public class A_test707 {
	boolean flag;
	public boolean foo() {
		return extracted();
	}
	protected boolean extracted() {
		/*[*/target: {
			do {
				if (flag)
					break target;
				return false;
			} while (flag);
		}
		return true;/*]*/
	}
}
