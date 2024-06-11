package branch_out;

import java.util.List;

public class A_test770 {

	public static void foo(List<List<String>> defs) {
		for (List<String> def : defs) {
			/*]*/
			extracted(def);
			/*[*/
		}
	}

	protected static void extracted(List<String> def) {
		boolean isLeftRecursive= false;
		for (String rule : def) {
			if (!rule.isEmpty()) {
				break;
			}
		}

		if (!isLeftRecursive) {
			return;
		}
	}
}

