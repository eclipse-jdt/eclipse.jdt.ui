package duplicates_in;

import java.util.List;

public class A_test964 {
	boolean test() {
		Object x= null;
		List l= null;
		if (true) {
			/*[*/if (x instanceof Integer)
				l.add(null);
			l.add(new Object());/*]*/
		} else {
			if (true)
				if (x instanceof Integer)
					l.add(null);
			l.add(new Object());
		}
		return true;
	}
}