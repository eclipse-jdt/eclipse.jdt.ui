package p;

import static p.A.m;

class A {
	public static void m() {}
}

class User {
	{
		A.m();
		m();
	}
}