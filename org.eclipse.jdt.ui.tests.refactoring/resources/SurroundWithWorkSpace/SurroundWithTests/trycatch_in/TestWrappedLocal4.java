package trycatch_in;

import java.io.File;

public class TestWrappedLocal4 {

	public void foo() {
		File file= null;
		
		/*]*/int i= 10, x; 
		file.toURL();/*[*/
		
		i= 20;
	}

}
