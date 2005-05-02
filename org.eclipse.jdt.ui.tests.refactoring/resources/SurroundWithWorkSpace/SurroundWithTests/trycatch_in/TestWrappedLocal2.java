package trycatch_in;

import java.io.File;

public class TestWrappedLocal2 {

	public void foo() {
		File file= null;

		/*[*/int i= 10; int x; 
		file.toURL();/*]*/
		i= 20;
	}
}
