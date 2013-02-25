package object_out;

public class TestNestedRead {
	private TestNestedRead field;
	public int i;
	
	public int foo() {
		return getField().getField().getField().getField().i;
	}

	public TestNestedRead getField() {
		return field;
	}

	public void setField(TestNestedRead field) {
		this.field = field;
	}
}

