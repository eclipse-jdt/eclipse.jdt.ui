import java.io.IOException;
import java.io.InputStreamReader;

public abstract class Z {
	public volatile boolean flag;
	
	public Object foo() throws NumberFormatException {
		InputStreamReader in= null;
		try {
			bar();
			return new Object();
		} catch (IOException e) {
			throw new NumberFormatException();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public abstract void bar() throws IOException;
}

