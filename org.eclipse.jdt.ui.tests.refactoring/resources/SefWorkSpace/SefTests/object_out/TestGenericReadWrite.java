package object_out;

public class TestGenericReadWrite<E extends String> {
	private E field;

	public void foo() {
		E temp = getField();
		setField(temp);
	}

	public void setField(E field) {
		this.field = field;
	}

	public E getField() {
		return field;
	}
}
