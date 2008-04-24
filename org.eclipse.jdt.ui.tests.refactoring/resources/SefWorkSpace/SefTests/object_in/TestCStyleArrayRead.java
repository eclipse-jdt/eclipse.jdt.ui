package object_in;

public class TestCStyleArrayRead {
	public Object field[] = new Object[0];

	public TestCStyleArrayRead() {
		field = new Object[0];
	}
	public void basicRun() {
		System.err.println(field.length);
	}

}
