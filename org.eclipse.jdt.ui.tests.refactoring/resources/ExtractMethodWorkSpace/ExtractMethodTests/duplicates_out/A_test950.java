package duplicates_out;

public class A_test950 {
	void f(){
		int i= 0;
		int j= 1;
		int k= extracted(i, j);

		int i1= 0;
		int j1= 1;
		int k1= extracted(i, j);
	}

	protected int extracted(int i, int j) {
		return /*[*/i+j/*]*/;
	}
}
