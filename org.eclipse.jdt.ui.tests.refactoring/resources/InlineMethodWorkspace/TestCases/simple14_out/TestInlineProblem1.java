public class TestInlineProblem1 {

	public static void main(String[] args) {
		var i_S = new InlineProblem1.I_S(1, "abc");
		System.out.println(i_S.i + i_S.s);
	}

	static record I_S(int i, String s) {}
}
