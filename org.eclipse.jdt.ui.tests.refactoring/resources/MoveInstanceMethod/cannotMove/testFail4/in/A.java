package p1;

import p2.B;

public class A {
	public int foob() {
		return 1;	
	}
	
	public static Child extends A {
		public int m(B b) {
			return super.foob();
		}
		
		public int foob() {
			return 2;	
		}
	}
}