package trycatch_in;

import java.io.File;

public class TestWrappedLocal8 {

	public void foo() {
		/*[*/final File file= null;
		file.toURL();/*]*/
		
		File file2= file;
	}

}
