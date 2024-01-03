public class Foo {
	Foo foo;

	int i= 0;

	public static void bar(Foo foo) {
		//Field Access
		foo.foo.foo.foo.method();
		foo.getInstance().getInstance().method();
		foo.foo.getInstance().foo.getInstance().foo.method();
		foo.getInstance().foo.getInstance().foo.getInstance().method();

		foo.foo.foo.foo.i++;
		foo.getInstance().getInstance().i++;
		foo.foo.getInstance().foo.getInstance().i++;
		foo.getInstance().foo.getInstance().foo.getInstance().i++;
		
		//Qualified Name
		foo.foo.foo.foo = foo.foo.foo;
	}

	public Foo getInstance() {
		return this;
	}

	public void method() {
	}
}
