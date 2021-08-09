package invalidSelection;

public class A_test198 {

	public void foo() {
		for (int j= 0; j < 3; j++) {
			/*[*/
			if(j == 3) {
				break;
			}
			System.out.println();
			/*]*/
		}
	}
}

