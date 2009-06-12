package p;

class A<E> {
	String x() {
		String t = bar();
		return t;
	}

	static <T> T bar() {
		return null;
	}
}
