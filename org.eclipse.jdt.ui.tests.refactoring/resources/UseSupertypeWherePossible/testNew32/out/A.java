package p;
//change to B
class B {
} 
class A extends B{
	void f(){
		B a= null;
		B a1= a;
		B a2= a1;
		A a3= null;
		a3.f();
	}
}
