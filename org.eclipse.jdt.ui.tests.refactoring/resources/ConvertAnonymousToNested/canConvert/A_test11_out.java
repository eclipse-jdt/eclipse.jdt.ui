package p;
class A{
	private static final class Inner extends A {
		int k;
		private final int u;

		public Inner(int u) {
			this.u = u;
			k= u;
		}
	}
	void f(){
		final int u= 9;
		new Inner(u);
	}
}