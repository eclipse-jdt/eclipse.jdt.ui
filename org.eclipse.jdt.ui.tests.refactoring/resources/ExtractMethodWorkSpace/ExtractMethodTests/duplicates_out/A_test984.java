package duplicates_out;

public class A_test984 {
	int[] x;
	int i = 1;
	int f(){
		return extracted();
	}
	protected int extracted() {
		return /*[*/i/*]*/;
	}
	void g() {
		x[extracted()]= 1;
	}
}
