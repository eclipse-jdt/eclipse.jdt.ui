package p;
class A {
	class Inner{
		void f(){
			A.this.m();
		}
	}
	void m(){}
}