package object_in;

public class TestGenericReadWrite<E extends String> {
	public E field;

	public void foo() {
		E temp = field;
		field = temp;
	}
}
