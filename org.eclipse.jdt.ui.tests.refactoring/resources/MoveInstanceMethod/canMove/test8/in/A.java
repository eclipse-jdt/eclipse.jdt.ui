package p1;

import p2.B;

public class A {
	
	public String foo= "foo";
	
	public Object m(B b) {
		System.out.println(foo);
		System.out.println(this.foo);
		System.out.println(b.bar);
		return null;
	}
}