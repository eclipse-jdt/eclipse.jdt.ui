package p5;

public class A {
	
	private class Inner {
		
		private int someField = 0;
		
		private class MoreInner {

			{
				someField++;
			}
		
		}
	}

}
