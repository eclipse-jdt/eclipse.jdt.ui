package invalidSelection;

public class A_test105 {
	public void foo() {
		try {
			foo();
		} catch (Exception e) /*]*/{
			foo();
		/*]*/}
	}
}