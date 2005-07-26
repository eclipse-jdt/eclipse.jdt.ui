package duplicates_in;

public class A_test964 {
	boolean test() {
		Object x= null;
		if (true) {
			/*[*/if (x instanceof Integer)
				return false;
			return true;/*]*/
		} else {
			{
				if (x instanceof Integer)
					return false;
			}
			return true;
		}
	}
}