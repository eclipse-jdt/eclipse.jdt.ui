package p;
class A {
	static void f(){}
	static class Inner{
		void foo(){
			f();
		}
	}
}