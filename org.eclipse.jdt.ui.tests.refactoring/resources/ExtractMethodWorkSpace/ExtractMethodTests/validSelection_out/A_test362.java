package validSelection_out;

public class A_test362 {
	A_test362(int i){
	}
	void n(){
		final int y= 0;
		extracted(y);
	}
	protected void extracted(final int y) {
		/*[*/new A_test362(y){
			void f(){
				int y= 9;
			}
		};/*]*/
	}
}
