package trycatch_in;

import java.io.File;

public class TestWrappedLocal7 {

	public void foo() {
		/*[*/File file= null;
		file.toURL();
		File file2= null;/*]*/
		file= null;
		file2= null;
	}

}
