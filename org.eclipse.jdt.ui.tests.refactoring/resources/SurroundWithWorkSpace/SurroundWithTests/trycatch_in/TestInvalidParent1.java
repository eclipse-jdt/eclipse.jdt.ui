package trycatch_in;

import java.io.File;

public class TestInvalidParent1 {
	public void foo() {
		File file= null;
		if (/*]*/file.toURL() == null/*[*/)
			return;
	}
}
