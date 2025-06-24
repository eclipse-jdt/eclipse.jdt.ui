package p;

public class Foo extends C<Integer> {
	/**
	 * @param foo
	 * @param i1
	 */
	public static void h(Foo foo, Integer i1) {
		foo.f(i1);
	}

	@Override
	void f(Integer i1) {
	}
}
