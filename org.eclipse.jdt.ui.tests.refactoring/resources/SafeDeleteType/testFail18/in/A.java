package p;
class A{
	class B{}
}
class C{
	void f(){
		new A().new B();
	}
}