public class Foo {
	private int counter;

	private InnerClass inner;

	private static int staticInt= 0;

	public void bar() {
		staticMethod();
		staticInt= staticInt + 1;
		int localInt= 0;
		this.counter= this.counter + 1;
		this.instanceMethod();
		this.inner.printHello();
	}

	public void instanceMethod() {
		this.bar();
	}

	public static void staticMethod() {
		Foo foo= new Foo();
		foo.bar();
	}

	private class InnerClass {
		public void printHello() {
			System.out.println("Hello from InnerClass!");
		}
	}
}
