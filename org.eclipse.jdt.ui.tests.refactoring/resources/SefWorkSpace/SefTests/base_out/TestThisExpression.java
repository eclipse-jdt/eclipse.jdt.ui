package base_out;

public class TestThisExpression {
	private int field;
	
	public void foo() {
		this.setField(10);
		new TestThisExpression().setField(11);
	}

	int getField() {
		return field;
	}

	void setField(int field) {
		this.field = field;
	}
}