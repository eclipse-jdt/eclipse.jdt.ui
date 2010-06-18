package object_out;

public class TestGenericRead<E> {
	private E field;

	public void foo() {
		E e = getField();
	}

	public E getField() {
		return field;
	}

	public void setField(E field) {
		this.field = field;
	}
}
