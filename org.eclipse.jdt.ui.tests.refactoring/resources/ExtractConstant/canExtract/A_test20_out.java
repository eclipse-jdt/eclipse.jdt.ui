// 7, 19 -> 7, 28
class BodyDeclOnSameLine {
	private final static String BAR= "c";
	private final static String FOO=  "a";  /* ambiguous */
	private static final String CONSTANT= FOO + BAR; String strange= "b"; //$NON-NLS-1$ //$NON-NLS-2$

	void m() {
		String s= CONSTANT;
	}
}