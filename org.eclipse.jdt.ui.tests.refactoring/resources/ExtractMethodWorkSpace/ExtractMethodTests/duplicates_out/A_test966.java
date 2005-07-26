package duplicates_out;

import java.util.List;

public class A_test964 {
	boolean test() {
		Object x= null;
		List l= null;
		if (true) {
			extracted(x, l);
		} else {
			if (true)
				if (x instanceof Integer)
					l.add(null);
			l.add(new Object());
		}
		return true;
	}

	protected void extracted(Object x, List l) {
		/*[*/if (x instanceof Integer)
			l.add(null);
		l.add(new Object());/*]*/
	}
}