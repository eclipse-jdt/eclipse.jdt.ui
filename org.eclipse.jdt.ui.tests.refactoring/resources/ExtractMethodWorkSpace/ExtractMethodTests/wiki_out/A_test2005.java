package wiki_out;

import java.io.IOException;

public class A_test2005 {

	static void fun() throws IOException {
	}

	public static void main(String args[]) {
		try {
			extracted();
		} catch (Exception e) {
		}
	}

	protected static void extracted() throws IOException {
		/*[*/
		fun();
		/*]*/
	}
}