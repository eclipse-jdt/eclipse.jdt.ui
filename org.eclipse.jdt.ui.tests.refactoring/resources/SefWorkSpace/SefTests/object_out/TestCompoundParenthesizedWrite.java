package object_out;

public class TestCompoundParenthesizedWrite {
	private String field;
	
	public void foo(TestCompoundParenthesizedWrite other) {
		other.setField("field");
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
}

