package p;

public class Annotation1_in {
	public void foo() {
		Cell1 c= new Cell1();
	}
}
@interface Preliminary { }
class Cell1 {
	@Preliminary public /*[*/Cell1/*]*/() { }
}
