package p;

public class A {
	protected void m() { 
		int i= B.ss();
	}	
}
class B extends A{
	public static int ss() { 
		return 9;
	}
}
