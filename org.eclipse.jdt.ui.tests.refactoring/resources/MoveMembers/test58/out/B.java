package p;

import p.A.Inner2;

public class B {
	
	private static class InnerTarget {
		
	}

	// move to B
	private static class Inner {
		String a= A.b;
		String e= new A.Inner2().c;
	}


}
