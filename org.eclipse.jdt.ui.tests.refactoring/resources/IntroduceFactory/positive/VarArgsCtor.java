package p;

public class VarArgsCtor_in {
	public void foo() {
		Cell c= Cell.createCell("", "");
	}
}
class Cell {
	public static Cell createCell(String... args) {
		return new Cell(args);
	}

	private /*[*/Cell/*]*/(String ... args) { }
}
