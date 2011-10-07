package object_out;

public class TestCStyleArrayRead {
	private Object field[] = new Object[0];

	public TestCStyleArrayRead() {
		setField(new Object[0]);
	}
	public Object[] getField() {
		return field;
	}
	public void setField(Object field[]) {
		this.field = field;
	}
	public void basicRun() {
		System.err.println(getField().length);
	}

}
