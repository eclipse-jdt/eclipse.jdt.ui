package p;

class A{
	class Inner{
	}
}
class B extends A{
	void f(){
		new Inner();
	}
}