package invalidSelection;

public class A_testIssue3223 {

	void test() {

		class Helper {
			int value = 10;

			int get() {
				return value;
			}
		}

		Helper helper = new Helper();

		/*]*/System.out.println(helper.get());/*[*/

	}


	public static void main(String[] args) {
		new A_testIssue3223().test();
	}
}
