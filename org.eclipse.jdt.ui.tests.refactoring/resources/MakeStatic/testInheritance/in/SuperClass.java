public class SuperClass {

	public String bar(String bar) {
		String i = bar;
		return i;
	}

	public static void staticMethod() {
		SuperClass instance = new SuperClass();
		String j = instance.bar("bar");
	}
}