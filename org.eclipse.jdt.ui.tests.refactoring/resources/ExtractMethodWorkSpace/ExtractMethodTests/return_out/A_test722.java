package return_out;

public class A_test722 {
	void f(){
		for (int i = 0; i < 10; i++) {
			extracted();
		}
	}

	protected void extracted() {
		/*[*/for (int j = 0; j < 10; j++) {
		}/*]*/
	}
}
