package p;
class A {
	public int i = 0;
}
class B extends A {
	public long test() {
		return new B().m(2, this);
	}

	public long m(int i, A a) {
		return a.i + i;
	}
}