public class SubClass extends SuperClass {

	@Override
	public String bar(String bar) {
		String i = super.bar(bar);
		return i;
	}
	
	public static void staticMethod() {
		SubClass instance = new SubClass();
		String j = instance.bar("bar");
	}
}