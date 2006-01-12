package p;

public class Foo<E> {

	static class Bar<F> {

		static class FooBar<G> {

			static <E, F, G, H> void foo(E e, F f, G g, H h) {

			}
		}
	}

	{
		Foo.Bar.FooBar.foo(null, null, null, null);	// <-- invoke here
	}

}
