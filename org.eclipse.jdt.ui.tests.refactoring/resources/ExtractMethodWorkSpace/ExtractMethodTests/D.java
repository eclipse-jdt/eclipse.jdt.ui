import java.io.IOException;
import java.net.MalformedURLException;

public class D {
	public static class MyException extends Exception {
	}
	
	public volatile boolean flag;
	
	protected void foo() {
		int i= 10;
		try {
			try {
				if (flag)
					throw new IOException();
				if (!flag)
					throw new MyException();
			} catch (IOException e) {
			}
			i= 10;
		} catch (MyException e) {
		}
		read(i);
	}

	private void read(int i) {
	}	
}

