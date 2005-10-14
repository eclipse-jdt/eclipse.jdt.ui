package p;

public class Annotation2_in {
	public void foo() {
		Cell2 c= Cell2.createCell2();
	}
}
@interface Buggy {
	String value();
}
class Cell2 {
	public static Cell2 createCell2() {
		return new Cell2();
	}

	@Buggy("doesn't work")
	private /*[*/Cell2/*]*/() { }
}
