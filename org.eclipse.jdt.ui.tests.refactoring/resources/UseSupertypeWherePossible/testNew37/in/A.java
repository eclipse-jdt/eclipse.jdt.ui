package p;

class B {}
class A extends B {}
class Test{
	void foo() {
		A a= null;		
		B b;
		b= (A) a;
	}
}