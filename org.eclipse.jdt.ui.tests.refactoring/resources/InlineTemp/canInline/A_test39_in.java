package p;

class A<E> extends Super {
	String x() {
		String t = super.bar();
		return t;
	}

	<T> T bar() {
		return null;
	}
}

class Super {
	<T> T bar() {
		return null;
	}
}