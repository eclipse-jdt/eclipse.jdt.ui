package p;
class A{
	private int a;
	public A(int a) {
		this.a = a;
	}
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
		super(4);
		this.a = a;
		this.b = b;
	}
}
