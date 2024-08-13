package p;

class A<E> {
	String x() {
		return bar();
	}

	<T> T bar() {
		return null;
	}
}
