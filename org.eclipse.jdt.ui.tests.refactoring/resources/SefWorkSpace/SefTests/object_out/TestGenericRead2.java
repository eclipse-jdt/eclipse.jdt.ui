package object_out;

public class TestGenericRead2<E> {
	private E field;

	public void setField(E field) {
		this.field = field;
	}

	public E getField() {
		return field;
	}
}

class UseTestGenericRead2 {
	public void foo() {
		TestGenericRead2<String> o= null;
		String e = o.getField();
	}
}
