package base_out;

public class TestFinal {
	private final int field= 0;
	
	public void foo() {
		int i= getField();
	}

	int getField() {
		return field;
	}
}
