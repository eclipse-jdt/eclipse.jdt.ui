package object_out;

public class TestSetterInInitialization {
	private String field;
	
	public void foo() {
		TestSetterInInitialization a= null;
		String t= a.setField("d");
	}

	String getField() {
		return field;
	}

	String setField(String field) {
		this.field = field;
		return field;
	}
}
