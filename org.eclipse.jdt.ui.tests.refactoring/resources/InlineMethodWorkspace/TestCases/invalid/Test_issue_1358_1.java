package invalid;

class MyClass {
	private static int value= 10;

	public static void staticMethod() {
		System.out.println("Value: " + value);
	}
}

class AnotherClass {
	private int value= 20;

	public void callStaticMethod() {
		new MyClass()./*]*/staticMethod()/*[*/;
	}
}
