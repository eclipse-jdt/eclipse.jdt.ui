package p;

import p2.I2;

public interface A extends I1{
	public static final int count= 10;
	public abstract void abstractM1();

	public void abstractM2();

	abstract void abstractM3();

	void abstractM4();

	public default void defaultM1() {
		System.out.println(count);
		System.out.println(message);
		System.out.println(I2.total);
	}

	public static void statictM1(String s) {
		System.out.println(s);
	}
}