package object_out;

public class TestCompoundWrite {
	private String field;
	
	public void foo() {
		setField(getField() + "d");
	}

	void setField(String field) {
		this.field = field;
	}

	String getField() {
		return field;
	}
}
