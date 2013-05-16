package object_out;

public class TestCompoundWrite4 {
	private int field;
	
	public void foo() {
		setField(getField() + 1 + 2);
	}

	int getField() {
		return field;
	}

	void setField(int field) {
		this.field = field;
	}
}
