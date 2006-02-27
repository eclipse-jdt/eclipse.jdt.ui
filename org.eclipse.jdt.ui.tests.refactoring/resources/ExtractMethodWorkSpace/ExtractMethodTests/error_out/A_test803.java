package error_out;

public class A_test803 {
	void m(String[] names) {
		extracted(names);
	}

	protected void extracted(String[] names) {
		/*[*/
		for (String string : names) {
			System.out.println(string.);
		}
		/*]*/
	}
	}
}
