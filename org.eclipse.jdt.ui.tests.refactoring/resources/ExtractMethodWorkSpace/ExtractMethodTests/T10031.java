public class T10031 {
	private static Object fValue;
	
	public static void foo() {
		setValue(null);
	}

	public static void setValue(Object value) {
		fValue= value;
	}

	public static Object getValue() {
		return fValue;
	}
}
