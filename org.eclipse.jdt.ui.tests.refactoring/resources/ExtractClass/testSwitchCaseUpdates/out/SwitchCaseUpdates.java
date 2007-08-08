package p;

public class SwitchCaseUpdates {
	private SwitchCaseUpdatesParameter parameterObject = new SwitchCaseUpdatesParameter();
	private void foo() {
		switch (parameterObject.getTest()) {
		case 5: parameterObject.setTest(7);
				break;
		case 7: parameterObject.setTest(5);
		}
	}
}
