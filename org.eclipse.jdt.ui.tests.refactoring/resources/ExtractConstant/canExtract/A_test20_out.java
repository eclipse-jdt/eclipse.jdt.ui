// 7, 19 -> 7, 28
class BodyDeclOnSameLine {
	private final static String BAR= "c";
	private final static String FOO=  "a"; private static final String CONSTANT= FOO + BAR;  /* ambiguous */ String strange= "b"; //$NON-NLS-1$ //$NON-NLS-2$

	void m() {
		String s= CONSTANT;
	}
}