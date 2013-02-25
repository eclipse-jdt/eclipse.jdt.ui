package object_out;

public class TestCompoundWrite {
	private String field;
	
	public void foo() {
		setField(getField() + "d");
	}

	String getField() {
		return field;
	}

	void setField(String field) {
		this.field = field;
	}
}
