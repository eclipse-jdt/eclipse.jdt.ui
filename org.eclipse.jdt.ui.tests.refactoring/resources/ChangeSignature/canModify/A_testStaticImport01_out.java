package p;

import static p.A.abc;

class A {
	public static void abc() {}
}

class User {
	{
		A.abc();
		abc();
	}
}