package p;

import java.util.HashMap;
import java.util.List;

public class Bar {

	/* (non-Javadoc)
	 * @see p.Foo#foo(java.util.List, java.util.HashMap)
	 */
	public static void bar(Foo target, List a, HashMap b) {
		target.foo(a, b);
	}

}
