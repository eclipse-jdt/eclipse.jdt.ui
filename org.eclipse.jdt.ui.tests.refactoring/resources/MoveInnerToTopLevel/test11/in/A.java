package p;
class A {
	static int F= 1;
	static class Inner{
		void foo() {
			F= 1;
			A.F= 2;
			p.A.F= 3;
		}
	}
}