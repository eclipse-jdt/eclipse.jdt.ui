class Foo {
	public void bar(Foo2 e) {
		e.method().forEach(this::bar);
	}

	interface Foo2 {
		List<Foo2> method();
	}
}
