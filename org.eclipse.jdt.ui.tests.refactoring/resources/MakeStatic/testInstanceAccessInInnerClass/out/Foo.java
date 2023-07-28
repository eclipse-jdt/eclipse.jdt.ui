class Foo {

	int i;

	public static void bar(Foo foo) {
		foo.i= 0;
	}

	class Inner {
		void method() {
			Foo.bar(Foo.this);
		}
	}
}
