package trycatch_in;

import java.io.File;

public class TestWrappedLocal6 {

	public void foo() {
		File file= null;

		/*[*/int a, i= 10, x;
		file.toURL();/*]*/
		i= 20;
	}

}
