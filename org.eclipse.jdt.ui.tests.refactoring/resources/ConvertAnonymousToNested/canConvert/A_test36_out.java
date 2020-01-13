package p;

public class E {

	private final class Bar extends Foo {
	}

	public abstract class Foo {
	}

	public static void main(String[] args) {
		final E e= new E();
		e.new Bar();
	}
}
