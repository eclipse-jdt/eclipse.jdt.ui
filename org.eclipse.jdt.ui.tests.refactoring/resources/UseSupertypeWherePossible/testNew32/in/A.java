package p;
//change to B
class B {
} 
class A extends B{
	void f(){
		A a= null;
		A a1= a;
		A a2= a1;
		A a3= null;
		a3.f();
	}
}
