package p;
class A{
	private final class Inner extends A {
		private final int u;
		int k= u;
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