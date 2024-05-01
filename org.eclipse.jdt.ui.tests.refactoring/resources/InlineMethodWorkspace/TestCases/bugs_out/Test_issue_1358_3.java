package bugs_in;

public class Test_issue_1358_3 {
	class MyClass {
		private int value= 10;
		
		public void method() {
			System.out.println("Value: " + value);
		}
	}
	private int value= 20;

	public void callMethod() {
		System.out.println("Value: " + new MyClass().value);
	}
}