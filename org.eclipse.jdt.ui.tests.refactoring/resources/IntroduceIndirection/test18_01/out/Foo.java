package p;

public interface Foo {
	
	/**
	 * @param foo
	 */
	public static void d(Foo foo) {
		foo.d();
	}

	default void d() {

	}

}
