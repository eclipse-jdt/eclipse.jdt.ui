package branch_in;

import java.util.List;

public class A_test770 {

	public static void foo(List<List<String>> defs) {
		for (List<String> def : defs) {
			/*]*/
			boolean isLeftRecursive= false;
			for (String rule : def) {
				if (!rule.isEmpty()) {
					break;
				}
			}

			if (!isLeftRecursive) {
				continue;
			}
			/*[*/
		}
	}
}

