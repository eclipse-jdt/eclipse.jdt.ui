package import_in;

public class TestStaticImport2 {
	public static final double PI= 3.14;
	void foo() {
		Provider p= null;
		p./*]*/useStaticImport()/*[*/;
	}
}
