package p;

interface A {
	public abstract void abstractM1();

	public void abstractM2();

	abstract void abstractM3();

	void abstractM4();

	public default void defaultM1(String s) {
		System.out.println(s);
	}

	public static void statictM1(String s) {
		System.out.println(s);
	}
}