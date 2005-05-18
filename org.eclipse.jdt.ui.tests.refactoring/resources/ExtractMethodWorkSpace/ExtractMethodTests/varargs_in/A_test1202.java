package varargs_in;

public class A_test1202 {
	public String foo(String... strings) {
		String s= "foo";
		/*[*/for (String string : strings) {
			System.out.println(string + s);
		}/*]*/
		return s;
	}
}
