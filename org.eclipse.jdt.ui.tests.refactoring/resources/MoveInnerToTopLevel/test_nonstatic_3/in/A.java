package p;

class A{
	class Inner{
	}
	
	static class Inner2{
		void f(){
			new A().new Inner();
		}	
	}
}