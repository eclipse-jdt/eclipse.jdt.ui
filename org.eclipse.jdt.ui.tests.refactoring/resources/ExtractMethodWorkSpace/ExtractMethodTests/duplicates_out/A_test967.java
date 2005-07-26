package duplicates_out;

import java.util.List;

public class A_test964 {
	public void test() {
		extracted();
		
		if (true)
			foo();
		else
			foo();
	}
	protected void extracted() {
		/*[*/foo();
		foo();/*]*/
	}
	public void foo() {
	}
}