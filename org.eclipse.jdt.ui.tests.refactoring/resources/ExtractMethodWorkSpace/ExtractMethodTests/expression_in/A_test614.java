package expression_in;

import java.io.File;

public class A_test614 {
	public void foo() {
		A a= null;
		a.useFile(/*[*/a.getFile()/*]*/);
	}
}
