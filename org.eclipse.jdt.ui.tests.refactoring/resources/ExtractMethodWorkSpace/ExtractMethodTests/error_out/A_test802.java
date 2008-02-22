package error_out;

public class A_test802 {
	public List g() {
		return null;
	}
	public void foo() {
		/*]*/extracted();
	}
	protected List extracted() {
		return g()/*]*/;
	}
}
