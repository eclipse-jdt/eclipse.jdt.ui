package p;

import static p.A.m;

class A {
	public static void m(Integer i, Object o) {}
}

class User {
	{
		A.m(1, null);
		m(2, null);
	}
}