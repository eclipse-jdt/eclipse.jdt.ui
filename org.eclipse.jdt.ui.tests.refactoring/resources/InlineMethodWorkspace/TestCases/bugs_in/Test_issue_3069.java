package bugs_in;

public class Test_issue_3069 {

	int x;

	public static void main(String[] args) {
		Test_issue_3069 obj= new Test_issue_3069();
		int result= obj.f();
	}

	int helper() {
		return 7;
	}

	int /*]*/ f()/*[*/ {
		class H {
			int y;
			int j() {
				return helper();
			}
			class K {
				int g() {
					return helper();
				}
				int h() {
					return g() + x + y;
				}
			}
		}
		return H.class.getSimpleName().length() + helper();
	}

}