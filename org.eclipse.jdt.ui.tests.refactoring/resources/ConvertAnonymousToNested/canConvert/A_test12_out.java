package p;
class A{
	private static final class Inner extends A {
		int k;
		private final int u;

		public Inner(int s, int u) {
			super(s);
			this.u = u;
			k= u;
		}
	}
	A(int x){
	}
	void f(){
		final int u= 9;
		int s= 2;
		new Inner(s, u);
	}
}