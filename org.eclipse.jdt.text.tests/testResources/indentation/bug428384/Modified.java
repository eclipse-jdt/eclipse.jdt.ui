package indentbug;

import java.util.ArrayList;
import java.util.List;

public class Bug {
	private void foo() {
		List<String> test =
				new ArrayList<>();
		System.out.println(test);
	}
}
