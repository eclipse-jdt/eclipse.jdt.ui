package p;

public class Annotation3_in {
	public void foo() {
		Cell3 c= Cell3.createCell3();
	}
}
@interface Authorship {
	String name();
	String purpose();
}
class Cell3 {
	public static Cell3 createCell3() {
		return new Cell3();
	}

	@Authorship(
		name="Rene Descartes",
		purpose="None whatsoever")
	private /*[*/Cell3/*]*/() { }
}
