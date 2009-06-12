package p;

class A<E> {
	String x() {
		return A.<String>bar();
	}

	static <T> T bar() {
		return null;
	}
}
