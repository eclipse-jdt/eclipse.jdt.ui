package p;

public class Annotation2_in {
	public void foo() {
		Cell2 c= new Cell2();
	}
}
@interface Buggy {
	String value();
}
class Cell2 {
	@Buggy("doesn't work") public /*[*/Cell2/*]*/() { }
}
