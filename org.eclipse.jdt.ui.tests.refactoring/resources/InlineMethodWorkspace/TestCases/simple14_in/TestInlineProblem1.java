public class TestInlineProblem1 {

	public static void main(String[] args) {
		var i_S = i_s();
		System.out.println(i_S.i + i_S.s);
	}

	static record I_S(int i, String s) {}

	private static I_S /*]*/i_s()/*[*/ {
		return new I_S(1, "abc");
	}
}
