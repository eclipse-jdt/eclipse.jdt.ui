package object_out;

public class TestSetterInExpression {
	private String field;
	
	public void foo() {
		TestSetterInExpression a= null;
		if ((a.setField("d")) == "d")
			foo();
	}

	String setField(String field) {
		return this.field = field;
	}

	String getField() {
		return field;
	}
}
