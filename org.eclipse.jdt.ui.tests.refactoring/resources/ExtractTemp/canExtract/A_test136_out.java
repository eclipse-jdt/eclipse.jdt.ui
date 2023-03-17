package p; // 4, 22, 4, 25
class A {
	String foo() {
		String m= m();
		String res = m;
		for (int i = 1; i < 10; ++i) {
			res = m();
		}
		return res;
	}
	
	String m() {
		B.S = B.S + "_";
		return B.S;
	}
}

class B {
	public static String S;
}