package branch_in;

public class A_test762 {

	public void foo() {
		outer: for (int i= 0; i < 3; i++) {
			/*[*/
			for (int j= 0; j < 3; j++) {
				for (int k= 0; k < 3; k++) {
					if(j == 3) {
						continue outer;
					}
					System.out.println();
				}
			}
			/*]*/
		}
	}
}

