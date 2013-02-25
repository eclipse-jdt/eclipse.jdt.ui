package object_out;

public class TestSimpleRead {
	private String field;
	
	public void foo() {
		String s= getField();
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
}

