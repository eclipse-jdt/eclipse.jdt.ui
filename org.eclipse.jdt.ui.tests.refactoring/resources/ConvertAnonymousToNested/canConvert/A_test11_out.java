package p;
class A{
	private final class Inner extends A {
		int k;
		private final int u;
		private Inner(int u) {
			super();
			this.u= u;
			k= u;
		}
	}

	void f(){
		final int u= 9;
		new Inner(u);
	}
}