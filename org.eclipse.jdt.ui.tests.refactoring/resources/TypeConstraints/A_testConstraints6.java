package p;
class A{
	A(A a){}
	void f(){
		A a0= null;
		A a1= new A(a0);
	}
}