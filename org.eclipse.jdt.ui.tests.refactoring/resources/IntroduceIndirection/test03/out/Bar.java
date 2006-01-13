package p;

import java.util.HashMap;
import java.util.List;

public class Bar {

	/* (non-Javadoc)
	 * @see p.Foo#foo(java.util.List, java.util.HashMap)
	 */
	public static void bar(Foo foo, List a, HashMap b) {
		foo.foo(a, b);
	}

}
