package duplicates_out;

public class A_test983 {
	int[] x;
	int i = 1;
	int[] f(){
		return extracted();
	}
	protected int[] extracted() {
		return /*[*/x/*]*/;
	}
	void g() {
		extracted()[i]= 1;
	}
}
