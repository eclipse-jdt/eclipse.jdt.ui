package invalidSelection;

import invalidSelection.*;

public class A_test063 {
	public void foo() {
		do/*[*/
			foo();
		/*]*/while(1 < 10);
	}
}