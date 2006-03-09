package rewrite_in;
/* params: arg
return Integer.parseInt(arg, 7);
 */

public class TestClassFile {
	int m(String arg) {
		return Integer./*]*/parseInt/*[*/(arg);
	}
}
