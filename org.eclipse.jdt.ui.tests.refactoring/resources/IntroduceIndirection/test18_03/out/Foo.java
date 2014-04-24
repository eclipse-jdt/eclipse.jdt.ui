package p;

public interface Foo {
	/**
	 * @param foo
	 */
	public static void a(Foo foo) {
		foo.a();
	}

	void a();
}
