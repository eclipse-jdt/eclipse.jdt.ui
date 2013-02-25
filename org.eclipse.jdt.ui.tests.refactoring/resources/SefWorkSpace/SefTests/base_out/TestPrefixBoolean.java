package base_out;

public class TestPrefixBoolean {
	private boolean field;
	
	public void foo() {
		boolean b;
		b= !isField();
	}

	boolean isField() {
		return field;
	}

	void setField(boolean field) {
		this.field = field;
	}
}
