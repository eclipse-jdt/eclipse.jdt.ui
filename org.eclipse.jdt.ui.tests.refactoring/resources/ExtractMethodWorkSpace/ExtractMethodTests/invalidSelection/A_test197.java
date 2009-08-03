package invalidSelection;

public class A_test197 {

	public void foo() {
		outer: for (int i= 0; i < 3; i++) {
			for (int j= 0; j < 3; j++) {
				/*[*/
				if(j == 3) {
					continue outer;
				}
				System.out.println();
				/*]*/
			}
		}
	}
}

