package p;

class A {
}

class B extends A {
	void m() { 
		int i= B.ss;
	}	
	public static int ss= 8;
}