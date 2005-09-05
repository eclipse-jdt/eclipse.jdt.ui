package p;
class A{
	private final class Inner extends A {
		private final int u;
		private Inner(int u) {
			this.u= u;
		}
		void g(){
			int uj= u;
		}
	}

	void f(){
		final int u= 9;
		new Inner(u);
	}
}