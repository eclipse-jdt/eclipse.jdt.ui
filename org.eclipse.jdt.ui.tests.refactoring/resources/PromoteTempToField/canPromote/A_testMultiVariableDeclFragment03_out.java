// 5, 72, 5, 72
package p;
class A {
	private double/*64*/ fC;

	void m() {
		@Unavailable(/*should implement*/"s")double/*64*/ a= 0, b= 1;
		fC= 2;
		@Unavailable(/*should implement*/"s")
		double/*64*/ d= 3;
	}
}
