package locals_in;

import java.util.List;
import java.util.ArrayList;

public class A_test579 {
	public static void foo(List<String> defs) {
		List<String> newDefs= new ArrayList<>();

		for (String rule : defs) {
			/*]*/boolean isLeftRecursive= false;
			if (!isLeftRecursive) {
				continue;
			}

			List<String> oldRules= new ArrayList<String>();
			List<String> newSuffixes= new ArrayList<String>();

			newDefs.addAll(oldRules);
			newDefs.addAll(newSuffixes);/*[*/
		}
	}
}
