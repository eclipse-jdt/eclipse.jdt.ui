package p;

class A{
	static void foo(){
	}
	class Inner {
		void f(){
			foo();
		}
	}
}