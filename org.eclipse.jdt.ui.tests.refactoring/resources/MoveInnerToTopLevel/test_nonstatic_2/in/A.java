package p;
class A{
	class Inner{
		void f(){
		}
	}
	class Inner2{
		void f(){
			new Inner();
		}
	}
}