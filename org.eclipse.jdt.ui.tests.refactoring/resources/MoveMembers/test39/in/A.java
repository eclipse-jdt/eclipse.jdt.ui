package p;

import other.C; //(-import)

public class A {
	Inner i; //+import r.B.Inner
	A.Inner ii; //+import r.B
	p.A.Inner iii;
	public static int a;
	public static class Inner { //move to r.B
		Inner buddy;
		public Inner(A.Inner other) {
					// ^ is direct access to enclosing type
			buddy= C.ii; //+import other.C
			int ia= a;
		}
	}
}
