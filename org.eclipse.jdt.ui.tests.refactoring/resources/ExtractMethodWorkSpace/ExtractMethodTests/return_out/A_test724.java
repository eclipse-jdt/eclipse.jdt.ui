package return_out;

public class A_test724 {
	protected void foo() {
		int i= 0;
		int j= 1;
		switch (j) {
			case 1 :
				i = extracted();
				break;
			default :
				i= -1;
				break;
		}
		System.out.println(i);
	}

	protected int extracted() {
		int i;
		/*[*/
		i= 1;/*]*/
		return i;
	}
}
