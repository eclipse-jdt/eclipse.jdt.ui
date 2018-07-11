package try10_in;

import java.io.Closeable;
import java.io.IOException;

public class A_testVar1 {
	public static void main(String[] args) {
		var x = 5;
		/*]*/var xStr = extracted(x);/*[*/     // (*)
		System.out.println(xStr);
	}

	protected static String extracted(int x) {
		var xStr = "x is " + x;
		return xStr;
	}
}
