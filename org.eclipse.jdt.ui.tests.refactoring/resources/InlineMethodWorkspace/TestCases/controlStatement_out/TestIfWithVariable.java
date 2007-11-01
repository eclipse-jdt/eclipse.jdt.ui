package controlStatement_out;

public class TestIfWithVariable {

	public boolean activate(String wizard) {
		return true;
	}

	public boolean activate(Object refactoring, String wizard) {
		String wizard1 = wizard;
		return activate(wizard1);
	}

	private void foo() {
		if (this != null) {
			String wizard1 = new String();
			activate(wizard1);
		}
	}
}