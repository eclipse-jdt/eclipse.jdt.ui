package p;
class A{
	class Inner{
		Inner(){
			f();
		}
		void f(){
		}
	}
	void f(){
		new Inner();
	}
}