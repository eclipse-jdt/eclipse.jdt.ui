package p;
class A {
	public static void method2(final int i) {
		final I x = new I() {//<-- refactor->convert local variable x to field
			public void methodI() {
				int y = 3;
			}
		};
		I y= x;
	}
}
interface I {
	void methodI();
}