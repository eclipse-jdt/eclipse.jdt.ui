package locals_out;

public class A_test564 {
	void foo(final int out){
		int i;
		if (out > 5){
			i = extracted();
		} else {
			i= 2;
		}
		i++;
	}

	protected int extracted() {
		int i;
		/*[*/i= 1;/*]*/
		return i;
	}
}
