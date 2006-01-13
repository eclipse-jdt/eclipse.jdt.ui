package p;

import java.io.IOException;

public class Foo extends Bar {
	
	/* (non-Javadoc)
	 * @see p.Bar#foo()
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
