package duplicates_out;

public class A_test980 {
	int x;
	int f(){
		return extracted();
	}
	protected int extracted() {
		return /*[*/x/*]*/;
	}
	void g() {
		x= 1;
	}
}
