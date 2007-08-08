package p;

public class SwitchCaseUpdates {
	private int test;
	private void foo() {
		switch (test) {
		case 5: test=7;
				break;
		case 7: test=5;
		}
	}
}
