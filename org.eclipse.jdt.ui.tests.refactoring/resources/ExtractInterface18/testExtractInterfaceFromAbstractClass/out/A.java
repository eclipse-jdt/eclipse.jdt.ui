package p;

public abstract class A implements B {	

	public abstract void abstractM();
	
	public void m1(String s) {
		System.out.println(s);
	}

	public static void statictM1(String s) {
		System.out.println(s);
	}
}