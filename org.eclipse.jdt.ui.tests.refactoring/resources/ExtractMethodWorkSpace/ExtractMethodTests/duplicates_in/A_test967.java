package duplicates_in;

import java.util.List;

public class A_test964 {
	public void test() {
		/*[*/foo();
		foo();/*]*/
		
		if (true)
			foo();
		else
			foo();
	}
	public void foo() {
	}
}