package invalidSelection;

import java.util.Enumeration;

public class A_test048 {
	public boolean flag;
	public void foo() {
		for (/*]*/Enumeration e= tests()/*[*/; e.hasMoreElements(); ) {
			if (flag)
				break;
		}
	}	
	public Enumeration tests() {
		return null;
	}
}