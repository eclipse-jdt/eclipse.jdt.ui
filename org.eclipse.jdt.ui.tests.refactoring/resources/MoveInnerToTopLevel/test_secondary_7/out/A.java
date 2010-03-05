package p;

import q.S;
import q.T;

public class A {
	S s= new S();
	T t= new T();
	class B {
		public B(){
			Secondary sec= new Secondary();
			sec.f(s);
		}
	}
}