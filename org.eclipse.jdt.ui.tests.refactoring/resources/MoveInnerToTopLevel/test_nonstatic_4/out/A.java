package p;

class A{
}
class B extends A{
	void f(){
		new Inner(this);
	}
}