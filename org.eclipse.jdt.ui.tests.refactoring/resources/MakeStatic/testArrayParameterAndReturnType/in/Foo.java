public class Foo {
	
	public String[] bar(String[] ending) {
		String[] j = new String[] {ending[0], ending[1]};
		return j;
	}
	
	public static void foo() {
		Foo instance = new Foo();
		String[] stringArray = new String[] {"bar", "bar"};
		String[] j = instance.bar(stringArray);
	}
}