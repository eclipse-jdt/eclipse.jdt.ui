package static_out;

public class TestStaticImportReadWrite {
	private static int x= 0;

	public static void setX(int x) {
		TestStaticImportReadWrite.x = x;
	}

	public static int getX() {
		return x;
	}
}
