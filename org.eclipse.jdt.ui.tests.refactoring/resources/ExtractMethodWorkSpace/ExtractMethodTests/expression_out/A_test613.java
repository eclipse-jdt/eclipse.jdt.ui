package expression_out;
public class A_test613 {
	public void foo() {
		A a= null;
		a.useFile(/*]*/extracted(a)/*[*/);
	}
	protected java.io.File extracted(A a) {
		return a.getFile();
	}
}
