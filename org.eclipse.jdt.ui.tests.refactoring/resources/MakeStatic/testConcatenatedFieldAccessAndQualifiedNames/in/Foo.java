public class Foo {
	Foo foo;

	int i= 0;

	public void bar() {
		//Field Access
		this.foo.foo.foo.method();
		this.getInstance().getInstance().method();
		foo.getInstance().foo.getInstance().foo.method();
		getInstance().foo.getInstance().foo.getInstance().method();

		this.foo.foo.foo.i++;
		this.getInstance().getInstance().i++;
		foo.getInstance().foo.getInstance().i++;
		getInstance().foo.getInstance().foo.getInstance().i++;
		
		//Qualified Name
		foo.foo.foo = foo.foo;
	}

	public Foo getInstance() {
		return this;
	}

	public void method() {
	}
}
