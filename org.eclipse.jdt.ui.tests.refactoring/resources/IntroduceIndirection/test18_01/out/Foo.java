package p;

public interface Foo {
	
	/**
	 * @param foo
	 */
	static void d(Foo foo) {
		foo.d();
	}

	default void d() {

	}

}
