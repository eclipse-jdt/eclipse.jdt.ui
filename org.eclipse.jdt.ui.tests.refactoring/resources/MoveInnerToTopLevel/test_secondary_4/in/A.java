package p;

class A{
	class Inner{
	}
}
class Secondary extends A{
	void f(){
		new Inner();
	}
}