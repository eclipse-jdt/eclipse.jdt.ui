package p1;

import p2.B;
import p1.a.T;

public class A {
	public B fB;

	/**
	 * This is a comment
	 * @param j a float
	 * @param foo a foo
	 * @param bar a bar
	 */
	public T mA1(float j, int foo, String bar) {
		System.out.println(bar + j);
		return new T();
	}
	
	public void mA2() {}
}