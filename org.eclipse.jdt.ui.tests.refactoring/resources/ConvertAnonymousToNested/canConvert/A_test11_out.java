package p;
class A{
	private final class Inner extends A {
		private final int u;
		int k;
		private Inner(int u) {
			this.u= u;
			k= u;
		}
	}

	void f(){
		final int u= 9;
		new Inner(u);
	}
}