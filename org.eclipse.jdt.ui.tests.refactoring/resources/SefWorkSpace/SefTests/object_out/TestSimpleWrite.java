package object_out;

public class TestSimpleWrite {
	private String field;
	
	public void foo() {
		setField("field");
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
}

