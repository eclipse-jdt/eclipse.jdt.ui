package object_out;

public class TestSimpleWrite {
	private String field;
	
	public void foo() {
		setField("field");
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getField() {
		return field;
	}
}

