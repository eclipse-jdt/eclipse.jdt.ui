public class Test {
	public int a(int x) {
		return b(x) * 2;
	}

	public int b(int x) {
		try {
			return c(x);
		} catch (Exception e) {
			return 0;
		}
	}

	public int c(int x) throws Exception {
		if (x <= 0)
			throw new Exception();
		return 1;
	}
}