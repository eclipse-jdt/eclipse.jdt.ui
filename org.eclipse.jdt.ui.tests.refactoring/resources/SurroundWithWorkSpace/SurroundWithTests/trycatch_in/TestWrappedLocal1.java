package trycatch_in;

import java.io.File;

public class TestWrappedLocal1 {

	public void foo() {
		File file= null;
		
		/*[*/int i= 10;
		file.toURL();/*]*/
		i= 20;
	}

}
