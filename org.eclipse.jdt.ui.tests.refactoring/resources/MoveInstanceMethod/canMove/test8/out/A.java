package p1;

import p2.B;

public class A {
	
	public String foo= "foo";

	/**
	 * m
	 * @param b
	 * @return Object
	 * @throws Exception
	 */
	public Object m(B b) throws Exception {
		return b.m(this);
	}
}