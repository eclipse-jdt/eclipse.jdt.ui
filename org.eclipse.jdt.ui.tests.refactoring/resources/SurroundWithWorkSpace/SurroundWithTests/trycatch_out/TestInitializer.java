package trycatch_out;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class TestInitializer {
    private static InputStream input;

    static {
		try {
			/*[*/input = new FileInputStream("myfile");/*]*/
		} catch (FileNotFoundException e) {
		}
    }
}
