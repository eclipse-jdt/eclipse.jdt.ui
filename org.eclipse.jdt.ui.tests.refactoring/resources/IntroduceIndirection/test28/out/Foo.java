package p;

import java.io.IOException;

public class Foo extends Bar {
	
	/**
	 * @param bar
	 * @throws IOException
	 */
	public static void bar(Bar bar) throws IOException {
		bar.foo();
	}

	protected void foo() {
		
	}
	
	void myFoo() throws Exception {
		Foo.bar(this);				// <-- invoke here
		Foo.bar(new Bar());
	}

}
