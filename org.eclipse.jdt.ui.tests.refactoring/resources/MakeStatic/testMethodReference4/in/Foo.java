class Foo {
	void method() {
		Runnable function= this::bar;
	}

	String s= "";

	public void bar() {
		this.s = "";
	}
}
