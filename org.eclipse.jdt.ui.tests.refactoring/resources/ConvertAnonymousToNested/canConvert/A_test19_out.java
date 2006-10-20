package p;
class A{
	private final class Inner extends A {
		private final int u;
		int l= 9;
		int p0= 2, k, k1;
		int l1= l+1, p, q;
		private Inner(int u) {
			this.u= u;
			k= u;
			k1= k;
			q= p+u;
		}
	}

	void f(){
		final int u= 8;
		new Inner(u);
	}
}