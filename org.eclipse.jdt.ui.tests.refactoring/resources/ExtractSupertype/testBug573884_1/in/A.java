package p;
class A{
}
class B extends A{
	public final int a;
	public final int b;
	public int geta() {
		return a;
	}
	public int getb() {
		return b;
	}
	public B(int a, int b) {
		this.a = a;
		this.b = b;
	}
}
