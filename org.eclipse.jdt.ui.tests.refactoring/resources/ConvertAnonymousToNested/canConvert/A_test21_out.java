package p;
class A {
	private static final class Inner extends A {
		public void bar() {
			return;
		}
	}

	public static void foo() {
		A foo= new Inner();
	}
}
