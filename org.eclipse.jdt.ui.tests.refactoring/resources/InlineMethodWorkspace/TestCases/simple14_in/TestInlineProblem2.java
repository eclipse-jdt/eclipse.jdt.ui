public class TestInlineProblem2 {

	public static void main(String[] args) {
		var i_S = i_s();
		System.out.println(i_S.toString());
	}

	static class I_S {}

	private static I_S /*]*/i_s()/*[*/ {
		return new I_S();
	}
}
