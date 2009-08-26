package duplicates_out;

public class A_test981 {
	int x;
	int f(){
		return extracted();
	}
	protected int extracted() {
		return /*[*/x/*]*/;
	}
	void g() {
		this.x= 1;
	}
}
