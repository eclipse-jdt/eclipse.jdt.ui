package object_in;

public class TestNestedRead {
	public TestNestedRead field;
	public int i;
	
	public int foo() {
		return field.field.field.field.i;
	}
}

