package locals_out;

public class A_test565 {
	void f(byte bytes) {
		String s= "k";
		extracted(bytes, s);
	}

	protected void extracted(byte bytes, String s) {
		/*[*/System.out.println(s + " " + bytes); /*]*/
	}
}
