package p;
class A{

	public static final int A = 0;

	public static void m() {
	}

	public static class X{
	}

	public @interface Y{
		String name() default "foo";
	}
}
class B extends A{
}