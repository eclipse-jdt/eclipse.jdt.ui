package p;

import java.io.IOException;

public class Foo {

	public static void foo() throws IOException, ArrayIndexOutOfBoundsException {

	}
	
	/**
	 * @throws IOException
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public static void bar() throws IOException, ArrayIndexOutOfBoundsException {
		Foo.foo();
	}

	void foo2() throws Exception {
		Foo.bar();	// <- invoke here
	}

}
