package expression_in;

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
		
		if (/*[*/scanner.x < selection.start && selection.start < scanner.y/*]*/) {
			g();
		}
	}

	public void g() {
	}
}
