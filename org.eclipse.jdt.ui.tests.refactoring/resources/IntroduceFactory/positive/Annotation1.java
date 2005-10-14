package p;

public class Annotation1_in {
	public void foo() {
		Cell1 c= Cell1.createCell1();
	}
}
@interface Preliminary { }
class Cell1 {
	public static Cell1 createCell1() {
		return new Cell1();
	}

	@Preliminary
	private /*[*/Cell1/*]*/() { }
}
