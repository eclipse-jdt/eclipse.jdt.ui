package p;

class A {
	void run() {
		System.out.println(Other.Inner.CONST_INNER);
	}
}

class Other {
	static class Inner {
		public static final int CONST_INNER= 18;
	}
}