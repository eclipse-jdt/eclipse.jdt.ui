package trycatch_in;

import java.io.File;

public class TestWrappedLocal3 {

	public void foo() {
		File file= null;

		int i;
		/*[*/int x;
		file.toURL();/*]*/
		i= 20;
	}

}
