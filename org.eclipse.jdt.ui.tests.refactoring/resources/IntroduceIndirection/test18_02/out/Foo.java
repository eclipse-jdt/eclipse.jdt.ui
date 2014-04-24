package p;

public interface Foo {
	
	static void s() {

	}

	/**
	 * 
	 */
	public static void s() {
		Foo.s();
	}

}
