package p;

public class Foo<E, F, G extends Comparable<E>> {

	/**
	 * @param <E>
	 * @param <F>
	 * @param <G>
	 * @param foo
	 */
	public static <E, F, G extends Comparable<E>> void bar(Foo<E, F, G> foo) {
		foo.foo();
	}

	void foo() {

	}

	class X implements Comparable<String> {

		public int compareTo(String o) {
			// TODO Auto-generated method stub
			return 0;
		}
	}

	{
		Foo<String, String, X> f = new Foo<String, String, X>();
		Foo.bar(f);	// <<-- invoke here
	}

}
