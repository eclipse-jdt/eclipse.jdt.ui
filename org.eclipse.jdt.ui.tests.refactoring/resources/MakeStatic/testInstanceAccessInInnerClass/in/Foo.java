class Foo {

	int i;

	public void bar() {
		i= 0;
	}

	class Inner {
		void method() {
			bar();
		}
	}
}
