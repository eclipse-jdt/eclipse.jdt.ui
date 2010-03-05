package p;

import q.S;

public class A {
	S s= new S();
	class B {
		public B(){
			Secondary sec= new Secondary();	
			sec.f(s);
		}
	}
}