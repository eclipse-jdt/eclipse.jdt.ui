package invalidSelection;

public class A_test108 {
	public void foo() {
		try {
			foo();
		} finally /*]*/{
			foo();
		/*]*/}
	}
}