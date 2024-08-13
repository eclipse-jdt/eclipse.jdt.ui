package bugs_in;

class B {
	public static void m() {
		System.out.println("b");
	}
}

public class A {

	public static int k;

	public static void f() {
		m();
		k= 3;
	}

	public static void m() {
		System.out.println("a");
	}

	public class D extends B {
		public static void t() {
			/*]*/A.m();
			k= 3;/*[*/
		}
	}

}
