package base_out;

public class TestPrefixBoolean {
	private boolean field;
	
	public void foo() {
		boolean b;
		b= !isField();
	}

	void setField(boolean field) {
		this.field = field;
	}

	boolean isField() {
		return field;
	}
}
