package p;


public class A {
	
	private static class SomeInner {
		
		private static String a;
		
		// move to top
		private static class Inner {
			
			private String b= a;
			
		}
	}
	
	{
		new SomeInner.Inner().b= "";
	}
	
}
