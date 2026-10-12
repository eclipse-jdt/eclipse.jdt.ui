package invalidSelection;

public class A_testIssue3236_2 {

	int test() {

        /*]*/var obj = new Object() {
            int value = 10;

            int get() {
                return value;
            }
        };/*[*/

        return obj.get() + obj.value;

	}


	public static void main(String[] args) {
		System.out.println(new A_testIssue3236_2().test());
	}
}
