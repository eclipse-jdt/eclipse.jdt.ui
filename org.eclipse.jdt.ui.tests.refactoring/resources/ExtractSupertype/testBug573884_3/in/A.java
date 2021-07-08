package p;
class A{
	private int a;
	public A(int a) {
		this.a = a;
	}
}
class B extends A{
	public final int a;
	public final boolean b;
	public int geta() {
		return a;
	}
	public boolean getb() {
		return b;
	}
	public B(int k, boolean l) {
		super(4);
		a = k;
		b = l;
	}
}
