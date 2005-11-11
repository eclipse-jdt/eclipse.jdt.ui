package p;

public class A {
	
	private static String b;
	
	// move to B
	private static class Inner {
		String a= b;
		String e= new Inner2().c;
	}
	
	private static class Inner2 {
		private String c;
	}
}
