package base_out;

public class TestPostfixInt {
	private int field;
	
	public void foo() {
		setField(getField() + 1);
		setField(getField() - 1);
	}

	void setField(int field) {
		this.field = field;
	}

	int getField() {
		return field;
	}
}
