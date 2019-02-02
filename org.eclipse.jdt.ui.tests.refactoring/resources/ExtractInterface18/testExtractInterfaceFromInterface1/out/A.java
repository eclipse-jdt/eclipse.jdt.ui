package p;

interface A extends B {
	public default void defaultM1(String s) {
		System.out.println(s);
	}

	public static void statictM1(String s) {
		System.out.println(s);
	}
}