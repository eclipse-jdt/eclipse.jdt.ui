package p;

public class VarArgsCtor_in {
	public void foo() {
		Cell c= new Cell("", "");
	}
}
class Cell {
	public /*[*/Cell/*]*/(String ... args) { }
}
