package p1;

import p2.B;

public class A {
	
	public String foo= "foo";
	
	public Object m(B b) {
		return b.m(this);
	}
}