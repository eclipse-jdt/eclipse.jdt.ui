package varargs_out;

public class A_test1202 {
	public String foo(String... strings) {
		String s= "foo";
		extracted(s, strings);
		return s;
	}

	protected void extracted(String s, String... strings) {
		/*[*/for (String string : strings) {
			System.out.println(string + s);
		}/*]*/
	}
}
