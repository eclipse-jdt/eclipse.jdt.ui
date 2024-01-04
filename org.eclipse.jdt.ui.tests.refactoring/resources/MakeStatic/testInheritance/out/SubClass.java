public class SubClass extends SuperClass {

	public static String bar(String bar, SubClass subClass) {
		String i = subClass.bar(bar);
		return i;
	}

	public static void staticMethod() {
		SubClass instance = new SubClass();
		String j = SubClass.bar("bar", instance);
	}
}