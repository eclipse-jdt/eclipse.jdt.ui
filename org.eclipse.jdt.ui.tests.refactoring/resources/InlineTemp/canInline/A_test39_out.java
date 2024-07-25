package p;

class A<E> extends Super {
	String x() {
		return super.bar();
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