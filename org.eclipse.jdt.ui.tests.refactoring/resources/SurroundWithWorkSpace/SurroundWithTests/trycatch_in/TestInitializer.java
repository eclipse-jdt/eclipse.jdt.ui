package trycatch_in;

import java.io.FileInputStream;
import java.io.InputStream;

public class TestInitializer {
    private static InputStream input;

    static {
		/*[*/input = new FileInputStream("myfile");/*]*/
    }
}
