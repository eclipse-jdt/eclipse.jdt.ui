package object_out;

public class TestSetterInExpression {
	private String field;
	
	public void foo() {
		TestSetterInExpression a= null;
		if ((a.setField("d")) == "d")
			foo();
	}

	String getField() {
		return field;
	}

	String setField(String field) {
		this.field = field;
		return field;
	}
}
