package p;

public class Test<T extends Test<T>> {
    /**
	 * @param test
	 * @param t
	 */
	public static <T extends Test<T>> void foo(Test<T> test, T t) {
		test.foo(t);
	}

	void foo(T t) {
    }

    void f(T t) {
        Test.foo(t, null);
    }
}