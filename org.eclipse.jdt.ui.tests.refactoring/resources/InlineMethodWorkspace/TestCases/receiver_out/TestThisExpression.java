package receiver_out;

public class TestThisExpression {
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
	void m(TestThisExpression t){
	}
}

class Client{
	void f(){
		TestThisExpression t= null;
		C c= null;
		c.m(t);
		class X {
			void foo() {
				foo();
			}
		}
	}
}