package p;

class B {}
class A extends B {}
class Test{
	void foo() {
		B a= null;		
		B b= (B) a;
	}
}