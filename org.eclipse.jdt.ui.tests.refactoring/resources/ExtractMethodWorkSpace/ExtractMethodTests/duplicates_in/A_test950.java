package duplicates_in;

public class A_test950 {
	void f(){
		int i= 0;
		int j= 1;
		int k= /*[*/i+j/*]*/;

		int i1= 0;
		int j1= 1;
		int k1= i+j;
	}
}
