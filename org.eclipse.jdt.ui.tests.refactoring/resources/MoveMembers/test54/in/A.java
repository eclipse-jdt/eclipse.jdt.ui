package p;

public class A {

	// move to B
	private static String a;

	// move to B
	private static void b() {
	}

	// move to B
	private static class C {
	}

	{
		a = "";
		b();
		C c = new C();
	}

}
