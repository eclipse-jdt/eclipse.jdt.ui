package p;

public class Foo<E, F, G extends Comparable<E>> {

	/* (non-Javadoc)
	 * @see p.Foo#foo(java.lang.Object, java.lang.Object, java.lang.Comparable)
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
