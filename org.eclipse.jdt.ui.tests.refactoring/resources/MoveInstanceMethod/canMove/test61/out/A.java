package p;

public class A {
	public static class X {
		public static void n() {
		}
	}
}

class B {

	void m() {
		A.X.n();
	}
	
}