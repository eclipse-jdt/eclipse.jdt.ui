package simple_in;

public class TestLabeledStatement {

	public static void main() {
		/*]*/foo();/*[*/
	}

	public static void foo() {
		the_label:
		while(true) {
			break the_label;
		}
	}
}
