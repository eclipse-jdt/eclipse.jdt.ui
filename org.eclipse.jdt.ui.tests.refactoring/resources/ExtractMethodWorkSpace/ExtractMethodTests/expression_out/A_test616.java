package expression_out;
public class A_test616 {
	public void foo() {
		A a= null;
		/*]*/extracted(a)/*[*/.getName();
	}
	protected java.io.File extracted(A a) {
		return a.getFile();
	}
}
