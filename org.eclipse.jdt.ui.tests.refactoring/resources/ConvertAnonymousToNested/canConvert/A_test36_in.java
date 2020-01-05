package p;

public class E {

	public abstract class Foo {
	}

	public static void main(String[] args) {
		final E e= new E();
		e.new Foo() {};
	}
}
