package validSelection;

import java.util.Enumeration;

public class A_test048_ {
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