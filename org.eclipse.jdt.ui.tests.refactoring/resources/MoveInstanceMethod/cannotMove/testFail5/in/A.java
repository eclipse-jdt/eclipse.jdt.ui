package p1;

import p2.B;

public class A {
	
	public class Inner {
		public void m(B b) {
			System.out.println(A.this);
		}	
	}
}