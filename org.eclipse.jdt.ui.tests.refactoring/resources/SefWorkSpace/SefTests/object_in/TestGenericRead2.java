package object_in;

public class TestGenericRead2<E> {
	public E field;
}

class UseTestGenericRead2 {
	public void foo() {
		TestGenericRead2<String> o= null;
		String e = o.field;
	}
}
