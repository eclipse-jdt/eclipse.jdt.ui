package p;
class A{
}
class B extends A{

	public static final int A = 0;

	public static void m() {
	}

	public static class X{
	}

	public @interface Y{
		String name() default "foo";
	}
}