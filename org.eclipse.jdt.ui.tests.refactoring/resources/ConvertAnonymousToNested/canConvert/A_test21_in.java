package p;
class A {
	public static void foo() {
		A foo= new A() {
			public void bar() {
				return;
			}
		};
	}
}
