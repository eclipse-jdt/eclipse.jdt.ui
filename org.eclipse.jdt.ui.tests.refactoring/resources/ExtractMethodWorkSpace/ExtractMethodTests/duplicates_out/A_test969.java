package duplicates_out;

import java.util.List;

public class A_test964 {
	public void test() {
		extracted();

		switch(10) {
			case 1:
				extracted();
		}
	}
	protected void extracted() {
		/*[*/foo();
		foo();/*]*/
	}
	public void foo() {
	}
}