package bugs_out;

public class Test_462038 {
	public static void main(String[] args) {
		Object [] keys = new Object [] {};
		int slot = 0;
		Object o;
		while (!/*]*/((o = keys[slot]) == null)/*[*/) {
			slot++;
		}
	}

	private static boolean isEmptyKey(Object object) {
		return object == null;
	}
}