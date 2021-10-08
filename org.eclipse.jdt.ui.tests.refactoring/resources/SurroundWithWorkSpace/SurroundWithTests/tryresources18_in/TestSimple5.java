package tryresources18_in;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

class TestSimple5 {
	void foo(boolean f1) throws IOException {
		File file = new File("somefile"); //$NON-NLS-1$
		if (f1) {
			/*[*/FileReader fileReader = new FileReader(file);/*]*/
			char[] in = new char[50];
			fileReader.read(in);
		}
	}
}
