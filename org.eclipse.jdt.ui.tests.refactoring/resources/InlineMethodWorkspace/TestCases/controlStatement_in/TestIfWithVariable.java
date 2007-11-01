package controlStatement_in;

public class TestIfWithVariable {

	public boolean activate(String wizard) {
		return true;
	}

	public boolean activate(Object refactoring, String wizard) {
		String wizard1 = wizard;
		return activate(wizard1);
	}

	private void foo() {
		if (this != null)
			/*]*/activate/*[*/(new Object(), new String());
	}
}