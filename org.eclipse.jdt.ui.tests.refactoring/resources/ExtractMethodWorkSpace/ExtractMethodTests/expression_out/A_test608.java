package expression_out;

public class A_test608 {

	public static class Scanner {
		public int x;
		public int y;
	}
	public static class Selection {
		public int start;
		public int end;
	}

	public void foo(Selection selection) {
		Scanner scanner= new Scanner();
		
		if (extracted(selection, scanner)) {
			g();
		}
	}

	protected boolean extracted(Selection selection, Scanner scanner) {
		return /*[*/scanner.x < selection.start && selection.start < scanner.y/*]*/;
	}

	public void g() {
	}
}
