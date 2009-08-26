package duplicates_out;

public class A_test982 {
	A_test982 c;
	int x;
	int f(){
		return extracted();
	}
	protected int extracted() {
		return /*[*/x/*]*/;
	}
	void g() {
		c.x= 1;
	}
}
