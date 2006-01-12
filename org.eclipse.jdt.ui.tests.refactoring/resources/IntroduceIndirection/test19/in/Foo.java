package p;

public class Foo<E, F, G extends Comparable<E>> {

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
		f.foo(null, null, null);	// <<-- invoke here
	}

}
