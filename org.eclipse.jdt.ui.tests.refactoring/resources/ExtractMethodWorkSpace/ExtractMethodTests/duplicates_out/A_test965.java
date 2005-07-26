package duplicates_out;

public class A_test964 {
	boolean test() {
		Object x= null;
		if (true) {
			return extracted(x);
		} else {
			{
				if (x instanceof Integer)
					return false;
			}
			return true;
		}
	}

	protected boolean extracted(Object x) {
		/*[*/if (x instanceof Integer)
			return false;
		return true;/*]*/
	}
}