package p;

import p.Foo.Bar.FooBar;

public class Foo<E> {

	static class Bar<F> {

		static class FooBar<G> {

			static <E, F, G, H> void foo(E e, F f, G g, H h) {

			}
		}
	}

	/**
	 * @param <E>
	 * @param <F>
	 * @param <G>
	 * @param <H>
	 * @param e
	 * @param f
	 * @param g
	 * @param h
	 */
	public static <E, F, G, H> void bar(E e, F f, G g, H h) {
		FooBar.foo(e, f, g, h);
	}

	{
		Foo.bar(null, null, null, null);	// <-- invoke here
	}

}
