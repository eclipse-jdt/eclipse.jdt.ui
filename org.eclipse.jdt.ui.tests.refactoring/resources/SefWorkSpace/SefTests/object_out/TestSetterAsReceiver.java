package object_out;

public class TestSetterAsReceiver {
	private String field;
	
	public void foo() {
		TestSetterAsReceiver a= null;
		(a.setField("d")).length();
	}

	String setField(String field) {
		return this.field = field;
	}

	String getField() {
		return field;
	}
}
