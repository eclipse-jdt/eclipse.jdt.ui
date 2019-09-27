package duplicates_out;

public class A_test992 {
	void a() {
		int xxx = extracted();
		System.out.println(xxx);
	}

	protected int extracted() {
		/*[*/int xxx= 0, yyy= 1;/*]*/
		return xxx;
	}
	
	void b() {
		int xxx= 0, yyy= 1;
		System.out.println(yyy);
	}
}