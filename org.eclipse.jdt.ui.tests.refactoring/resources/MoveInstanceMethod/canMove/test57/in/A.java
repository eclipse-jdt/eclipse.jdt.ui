package p;
class A {
	public int i = 0;

	public long m(int i, B b) {
		return this.i + i;
	}
}
class B extends A {
	public long test() {
		return super.m(2, new B());
	}
}