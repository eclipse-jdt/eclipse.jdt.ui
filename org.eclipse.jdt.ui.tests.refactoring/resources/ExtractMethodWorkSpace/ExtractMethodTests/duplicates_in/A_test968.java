package duplicates_in;

import java.util.List;

public class A_test964 {
	public void test() {
		/*[*/foo();
		foo();/*]*/

		switch(10) {
			case 1:
				foo();
			case 2:
				foo();
		}
	}
	public void foo() {
	}
}