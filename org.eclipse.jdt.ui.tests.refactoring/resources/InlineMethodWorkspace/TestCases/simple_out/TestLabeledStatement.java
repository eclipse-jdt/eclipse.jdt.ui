package simple_out;

public class TestLabeledStatement {

	public static void main() {
		the_label:
		while(true) {
			break the_label;
		}
	}

	public static void foo() {
		the_label:
		while(true) {
			break the_label;
		}
	}
}
