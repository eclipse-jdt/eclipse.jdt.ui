public class Foo {

	String j= "";

	public String bar(String foo) {
		String i= this.j;
		return i;
	}

	public void method() {
		String j= this.bar("bar");
	}
}
