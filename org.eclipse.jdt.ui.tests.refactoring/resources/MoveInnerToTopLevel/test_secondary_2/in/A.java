package p;

public class A {
	class B {
		void B(){
			Secondary s= new Secondary();
			
		}
	}
}
final class Secondary {
	void f(){
		new A().new B();
	}
}