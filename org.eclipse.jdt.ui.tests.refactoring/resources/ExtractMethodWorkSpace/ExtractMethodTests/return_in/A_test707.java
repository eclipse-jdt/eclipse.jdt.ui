package return_in;

public class A_test707 {
	boolean flag;
	public boolean foo() {
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
