package p;

import java.io.IOException;

public class Foo {

	public static void foo() throws IOException, ArrayIndexOutOfBoundsException {

	}
	
	void foo2() throws Exception {
		foo();	// <- invoke here
	}

}
