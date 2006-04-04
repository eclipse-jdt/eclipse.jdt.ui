package bugs_out;

public class Test_123356 {
	static String[] field;

	public static void main(String[] args) {
		/*]*/int x = (field = args).length;
		int y = field.hashCode();
		int add = y + x;/*[*/
	}

	static int add(int x, int y) {
		return y + x;
	}
}
