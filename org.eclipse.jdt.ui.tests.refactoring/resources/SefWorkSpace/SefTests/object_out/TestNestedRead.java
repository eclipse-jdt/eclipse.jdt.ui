package object_out;

public class TestNestedRead {
	private TestNestedRead field;
	public int i;
	
	public int foo() {
		return getField().getField().getField().getField().i;
	}

	public void setField(TestNestedRead field) {
		this.field = field;
	}

	public TestNestedRead getField() {
		return field;
	}
}

