package object_out;

public class TestSetterInInitialization {
	private String field;
	
	public void foo() {
		TestSetterInInitialization a= null;
		String t= a.setField("d");
	}

	String setField(String field) {
		return this.field = field;
	}

	String getField() {
		return field;
	}
}
