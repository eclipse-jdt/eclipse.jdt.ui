package p;
class A {
	class Inner{
		void f(){
			A.this.m= 1;
		}
	}
	int m;
}