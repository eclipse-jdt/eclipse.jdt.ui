package p;
class A {
	private static final I fgX= new I() {//<-- refactor->convert local variable x to field
		public void methodI() {
			int y = 3;
		}
	};

	public static void method2(final int i) {
		I y= fgX;
	}
}
interface I {
	void methodI();
}