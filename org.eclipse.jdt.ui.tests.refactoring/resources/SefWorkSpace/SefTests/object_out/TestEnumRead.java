package object_out;

public enum TestEnumRead {
	TEST;
	private String field;

	public void foo() {
		String s = getField();
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
}
