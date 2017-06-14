package duplicates_in;

public class A_test990 {
	public static void main(String[] args) {
		for (int i = 0; i < 1; i++) {
			int idx = 0;
			for (int j = 0; j < 3; j++) {
				idx++;
				System.out.println(idx);
			}
			// for-loop to extract:
			/*[*/for (int j = 0; j < 3; j++) {
				idx++;
				System.out.println(idx);
			}/*]*/
		}
	}
}