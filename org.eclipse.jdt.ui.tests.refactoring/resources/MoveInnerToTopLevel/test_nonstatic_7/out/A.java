package p;

class A{
	class X extends A{
		void f(){
			new Inner(this);
		}
	}
}