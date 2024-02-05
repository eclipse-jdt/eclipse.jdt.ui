class Foo {
	public static void bar(Foo foo) {
		foo.new Inner();
	}

	private class Inner {
	}
}
