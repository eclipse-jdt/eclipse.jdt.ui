package p;
class A{
	void f(){
		final int u= 9;
		new Inner(u);
	}

	private static final class Inner extends A {
		int k;
		private final int u;

		public MyA(int u) {
			this.u = u;
			k= u;
		}
	}
}