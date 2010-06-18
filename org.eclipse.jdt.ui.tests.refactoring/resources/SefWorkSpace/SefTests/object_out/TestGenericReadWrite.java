package object_out;

public class TestGenericReadWrite<E extends String> {
	private E field;

	public void foo() {
		E temp = getField();
		setField(temp);
	}

	public E getField() {
		return field;
	}

	public void setField(E field) {
		this.field = field;
	}
}
