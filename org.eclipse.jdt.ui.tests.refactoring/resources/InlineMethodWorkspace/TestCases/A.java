public class A {
	void m(C c){
		c.m(this);
		class X {
			void foo() {
				foo();
			}
		}
	}
}
class C {
	void m(A a){
		//method
	}
}
class Client{
	void f(){
		A a= null;
		C c= null;
		a.m(c);
	}
}
