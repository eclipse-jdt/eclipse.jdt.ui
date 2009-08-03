package branch_in;

public class A_test764 {

	public void foo() {
		int x = 0;
		for (int i= 0; i < 3; i++) {
			/*[*/
			if(i == 2) {
				x = 2;
				continue;
			}
			System.out.println();
			/*]*/
		}
		System.out.println(x);
	}
}

