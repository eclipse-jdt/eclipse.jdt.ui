package invalidSelection;

public class A_testIssue1516 {

	public void extracted() {
		System.out.println("c");
	}

	public void foo() {
		class D extends B {
			public void t() {
				extracted();
			}
		}
	}

}

class B {
	public void s() {
		/*]*/System.out.println("b");/*[*/
	}
}