package p;

import static p.A.m;

class A {
	public static void m(Integer i) {}
}

class User {
	{
		A.m(1);
		m(2);
	}
}