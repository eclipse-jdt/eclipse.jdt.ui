package trycatch17_out;
import java.io.IOException;
import java.net.MalformedURLException;

public class TestMultiTryCatch {
	private static void call() throws Exception, IOException, MalformedURLException {
		
	}
	
	public static void main(String[] args) {
		try {
			/*[*/call();/*]*/
		} catch (Exception e) {
		}
	}

}