package trycatch_in;

import java.io.File;

public enum TestEnum2 {
	A {
		public void foo() {
			File file= null;
			/*[*/file.toURL();/*]*/
		}
	};
}
