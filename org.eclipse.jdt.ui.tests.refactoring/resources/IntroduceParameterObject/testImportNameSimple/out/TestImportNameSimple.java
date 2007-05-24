package p;

public class TestImportNameSimple {
	public static void main(String[] args) {
		foo(new ArrayList(5, 6));
	}

	public static void foo(ArrayList p) {
		System.out.println(p.x + ", " + p.y);
	}
}
