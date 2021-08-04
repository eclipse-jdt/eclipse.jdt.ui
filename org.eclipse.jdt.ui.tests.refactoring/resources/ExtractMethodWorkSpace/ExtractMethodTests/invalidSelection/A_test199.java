package invalidSelection;

public class A_test199 {

	public void foo() {
		for (int j= 0; j < 3; j++) {

			if(j == 1) {
			/*]*/continue;/*[*/
			}
			System.out.println();

		}
	}
}

