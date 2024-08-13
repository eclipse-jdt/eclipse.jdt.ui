package p;

class A<E> {
	String x() {
		return bar();
	}

	static <T> T bar() {
		return null;
	}
}
