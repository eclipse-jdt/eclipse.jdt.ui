package object_in;

public class TestGenericRead<E> {
	public E field;

	public void foo() {
		E e = field;
	}
}
