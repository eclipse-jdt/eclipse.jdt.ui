import java.util.Map;

class Foo<T extends Map<? extends Runnable, ? extends Throwable>> {

	int j;

	public void bar() {
		this.j= 0;
	}
}
