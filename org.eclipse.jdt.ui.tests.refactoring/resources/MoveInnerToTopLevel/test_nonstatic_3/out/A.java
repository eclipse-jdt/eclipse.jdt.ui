package p;

class A{
	static class Inner2{
		void f(){
			new Inner(new A());
		}	
	}
}