package locals_in;

public class A_test553 {

	public void foo() {
		int i= 0;
		for (int x= i++, y= x;true;) {
			/*[*/x= i;/*]*/
		}
	}
}

