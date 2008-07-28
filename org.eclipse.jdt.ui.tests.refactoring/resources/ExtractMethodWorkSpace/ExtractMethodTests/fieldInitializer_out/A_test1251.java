package fieldInitializer_out;

public class A_test1251 {
	public static final String WORLD= "World";
	public static int fgAnswer= 42 + extracted();
	protected static int extracted() {
		return /*[*/("Hello" + ' ' + WORLD).length()/*]*/;
	}
}
