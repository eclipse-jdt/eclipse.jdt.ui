// 6, 29, 6, 29
package p;
class A {
	void m() {
		@Ann(value=0)
		final double/*64*/ a= 0, b= 1, c= 2, d= 3;
	}
}
@interface Ann {
	int value();
}
