package object_out;

public enum TestEnumReadWrite {
	TEST;
	private String field;

	public void foo() {
		setField(getField() + "field");
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
