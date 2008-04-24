package object_out;

public class TestSetterInAssignment {
	private String field;
	
	public void foo() {
		TestSetterInAssignment a= null;
		String t= null;
		t= a.setField("d");
	}

	String setField(String field) {
		this.field = field;
		return field;
	}

	String getField() {
		return field;
	}
}
