package p;
class A {
	private final class Inner extends A {
	}

	public static void foo1(){
		A foo = new A()	{
			public void foo(){
				A foo = new Inner();
			}
		};
	}
}
