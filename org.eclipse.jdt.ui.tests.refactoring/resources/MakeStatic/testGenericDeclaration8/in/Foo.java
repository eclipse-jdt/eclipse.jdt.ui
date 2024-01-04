import java.io.Serializable;

class Foo<A extends Throwable, B extends Runnable, C extends Runnable & Serializable> {

	int j;

	public void bar() {
		this.j= 0;
	}
}
