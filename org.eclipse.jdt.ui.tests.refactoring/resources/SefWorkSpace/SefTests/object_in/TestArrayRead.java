package object_in;

public class TestArrayRead {
	private Object[] field;

	public TestArrayRead() {
		field= new Object[0];
	}
	public void basicRun() {
		System.err.println(field.length);
	}
}
