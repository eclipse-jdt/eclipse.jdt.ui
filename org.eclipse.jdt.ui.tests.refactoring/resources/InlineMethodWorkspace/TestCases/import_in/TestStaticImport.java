package import_in;

public class TestStaticImport {
	void foo() {
		Provider p= null;
		p./*]*/useStaticImport()/*[*/;
	}
}
