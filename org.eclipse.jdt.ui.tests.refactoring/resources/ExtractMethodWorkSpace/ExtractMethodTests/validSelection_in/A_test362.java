package validSelection_in;

public class A_test362 {
	A_test362(int i){
	}
	void n(){
		final int y= 0;
		/*[*/new A_test362(y){
			void f(){
				int y= 9;
			}
		};/*]*/
	}
}
