package base_out;

public class TestPrefixInt {
	private int field;
	
	public void foo() {
		setField(getField() + 1);
		setField(getField() - 1);
		int i;
		i= +getField();
		i= - getField();
		i= ~getField();
	}

	void setField(int field) {
		this.field = field;
	}

	int getField() {
		return field;
	}
}
