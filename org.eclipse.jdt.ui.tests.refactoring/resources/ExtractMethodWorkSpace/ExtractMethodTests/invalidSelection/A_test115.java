package invalidSelection;

public class A_test115 {
	public void foo() {
		try {
			foo();
		} catch /*]*/(Exception e)/*[*/ {
			foo();
		}
	}
}