package p;

public class Foo<E, F, G extends Comparable<E>> {

	/**
	 * @param <E>
	 * @param <F>
	 * @param <G>
	 * @param foo
	 * @param e
	 * @param f
	 * @param g
	 */
	public static <E, F, G extends Comparable<E>> void bar(Foo<E, F, G> foo, E e, F f, G g) {
		foo.foo(e, f, g);
	}

	void foo(E e, F f, G g) {

	}

	class X implements Comparable<String> {

		public int compareTo(String o) {
			// TODO Auto-generated method stub
			return 0;
		}
	}

	{
		Foo<String, String, X> f = new Foo<String, String, X>();
		Foo.bar(f, null, null, null);	// <<-- invoke here
	}

}
