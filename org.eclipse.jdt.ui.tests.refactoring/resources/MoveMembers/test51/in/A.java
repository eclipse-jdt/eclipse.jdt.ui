package p;

public class A {
	
	private static class Inner {
		private void foo() {};
	}
	
	{
		new Inner().foo();
	}

}
