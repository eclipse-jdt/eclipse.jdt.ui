// 6, 29, 6, 29
package p;
class A {
	private double/*64*/ fA;

	void m() {
		fA= 0;
		@Ann(value=0)
		final
		double/*64*/ b= 1, c= 2, d= 3;
	}
}
@interface Ann {
	int value();
}
