package object_out;

public class TestSimpleRead {
	private String field;
	
	public void foo() {
		String s= getField();
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getField() {
		return field;
	}
}

