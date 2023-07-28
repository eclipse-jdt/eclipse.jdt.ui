import java.util.List;

class Foo<T extends List<?>> {

	int j;

	public void bar() {
		this.j= 0;
	}
}
