package p;
class A {
	static int F= 1;
	static class Inner{
		void foo() {
			F= 2;
		}
	}
}