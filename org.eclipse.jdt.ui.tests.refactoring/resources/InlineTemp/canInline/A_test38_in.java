package p;

class A<E> {
	String x() {
		String t = bar();
		return t;
	}

	<T> T bar() {
		return null;
	}
}
