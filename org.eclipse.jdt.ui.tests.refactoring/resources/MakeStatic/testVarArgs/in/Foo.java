public class Foo {

	int j;

	public void bar(String... items) {
		this.j= 0;
	}

	public void baz() {
		Foo foo= new Foo();
		foo.bar("A", "B", "C");
	}
}
