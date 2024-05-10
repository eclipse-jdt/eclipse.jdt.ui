package bugs_in;

class MyClass {
	static int value= 10;
	
	public static void staticMethod() {
		System.out.println("Value: " + value);
	}
}

public class Test_issue_1358_2 {
	private int value= 20;

	public void callStaticMethod() {
		System.out.println("Value: " + MyClass.value);
	}
}