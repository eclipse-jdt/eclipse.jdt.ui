package p;
class A{
	class Inner2{
		void f(){
			new Inner(A.this);
		}
	}
}