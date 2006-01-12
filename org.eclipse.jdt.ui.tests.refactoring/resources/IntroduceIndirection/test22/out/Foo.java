package p;

import p.Foo.Bar.FooBar;

public class Foo<E> {

	static class Bar<F> {

		static class FooBar<G> {

			static <E, F, G, H> void foo(E e, F f, G g, H h) {

			}
		}
	}

	/* (non-Javadoc)
	 * @see p.Foo.Bar.FooBar#foo(java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public static <E, F, G, H> void bar(E e, F f, G g, H h) {
		FooBar.foo(e, f, g, h);
	}

	{
		Foo.bar(null, null, null, null);	// <-- invoke here
	}

}
