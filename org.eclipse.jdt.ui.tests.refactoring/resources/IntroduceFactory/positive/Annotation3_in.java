package p;

public class Annotation3_in {
	public void foo() {
		Cell3 c= new Cell3();
	}
}
@interface Authorship {
	String name();
	String purpose();
}
class Cell3 {
	@Authorship(
		name="Rene Descartes",
		purpose="None whatsoever")
	public /*[*/Cell3/*]*/() { }
}
