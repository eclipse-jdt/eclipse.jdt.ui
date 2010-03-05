package p;

import q.S;
import q.T;

public class A {
	
	class B {
		public B(){
			Secondary sec= new Secondary();
			sec.f();
			sec.g();
		}
	}
}

final class Secondary {
	void f(){
		S s= new S();
	}
	void g(){
		T t= new T();
	}
}