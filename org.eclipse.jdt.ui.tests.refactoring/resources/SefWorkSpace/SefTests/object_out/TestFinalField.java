package object_out;

public class TestFinalField {
	private final Object field;
	TestFinalField(Object y) {
		field = y;
	}
	private Object getField() {
		return field;
	}
}
