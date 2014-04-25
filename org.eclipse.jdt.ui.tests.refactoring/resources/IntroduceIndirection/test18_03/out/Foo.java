package p;

public interface Foo {
	/**
	 * @param foo
	 */
	static void a(Foo foo) {
		foo.a();
	}

	void a();
}
