package invalidSelection;

public class A_test113 {
	public void foo() {
		/*]*/try {
			foo();
		}/*[*/ catch (Exception e) {
			foo();
		}
	}
}