package base_out;

public class TestPrefixBoolean {
	private boolean field;
	
	public void foo() {
		boolean b;
		b= !getField();
	}

	void setField(boolean field) {
		this.field = field;
	}

	boolean getField() {
		return field;
	}
}
