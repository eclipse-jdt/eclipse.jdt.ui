package bugs_in;

public class Test_issue_3069 {

	int x;

	public static void main(String[] args) {
		Test_issue_3069 obj= new Test_issue_3069();
		class H {
			int y;
			int j() {
				return obj.helper();
			}
			class K {
				int g() {
					return obj.helper();
				}
				int h() {
					return g() + obj.x + y;
				}
			}
		}
		int result= H.class.getSimpleName().length() + obj.helper();
	}

	int helper() {
		return 7;
	}

}