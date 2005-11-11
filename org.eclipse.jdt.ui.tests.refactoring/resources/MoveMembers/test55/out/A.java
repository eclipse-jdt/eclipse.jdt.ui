package p;

public class A {

	// move to B
	private static String a;

	// move to B
	private static void b() {
	}

	{
		a = "";
		b();
		B.C c = new B.C();
	}

}
