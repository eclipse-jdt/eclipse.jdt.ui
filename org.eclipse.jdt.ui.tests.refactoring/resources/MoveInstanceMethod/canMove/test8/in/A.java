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
		System.out.println(foo);
		System.out.println(this.foo);
		System.out.println(b.bar);
		return null;
	}
}