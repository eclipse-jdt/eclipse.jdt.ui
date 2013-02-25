package object_out;

public class TestArrayRead {
	private Object[] field;

	public TestArrayRead() {
		setField(new Object[0]);
	}
	private Object[] getField() {
		return field;
	}
	private void setField(Object[] field) {
		this.field = field;
	}
	public void basicRun() {
		System.err.println(getField().length);
	}
}
