package invalidSelection;

public class A_testIssue1758 {
	public void foo() {
		/*]*/int i;/*[*/
	}
}

class SubClass {
	void extracted() {
	}

	class InnerClass extends A_testIssue1758 {
		void testMethod() {
			extracted();
		}
	}
}
