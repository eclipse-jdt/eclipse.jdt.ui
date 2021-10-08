package tryresources18_in;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

class TestAnonymous1 {
	void foo(boolean f1) throws IOException {
		final File file = new File("somefile");
		try (/*[*/ FileReader fileReader = new FileReader(file)) {
			char[] in = new char[50];
			fileReader.read(in);
			new Runnable() {
				public void run() { 
					try {
						fileReader.close();
					} catch (IOException ex) {}
				}}.run();
		}
	}
}
