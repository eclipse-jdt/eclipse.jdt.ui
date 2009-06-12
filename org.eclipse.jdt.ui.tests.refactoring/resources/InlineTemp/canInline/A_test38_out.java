package p;

class A<E> {
	String x() {
		return this.<String>bar();
	}

	<T> T bar() {
		return null;
	}
}
