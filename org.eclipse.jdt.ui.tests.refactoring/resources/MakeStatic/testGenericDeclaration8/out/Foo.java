import java.io.Serializable;

class Foo<A extends Throwable, B extends Runnable, C extends Runnable & Serializable> {

	int j;

	public static <A extends Throwable, B extends Runnable, C extends Runnable & Serializable> void bar(Foo<A, B, C> foo) {
		foo.j= 0;
	}
}
