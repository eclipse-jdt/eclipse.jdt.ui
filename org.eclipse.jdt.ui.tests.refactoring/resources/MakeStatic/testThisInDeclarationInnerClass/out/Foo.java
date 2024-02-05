public class Foo {
	private int counter;

	private InnerClass inner;

	private static int staticInt= 0;

	public static void bar(Foo foo) {
		staticMethod();
		staticInt= staticInt + 1;
		int localInt= 0;
		foo.counter= foo.counter + 1;
		foo.instanceMethod();
		foo.inner.printHello();
	}

	public void instanceMethod() {
		Foo.bar(this);
	}

	public static void staticMethod() {
		Foo foo= new Foo();
		Foo.bar(foo);
	}

	private class InnerClass {
		public void printHello() {
			System.out.println("Hello from InnerClass!");
		}
	}
}
