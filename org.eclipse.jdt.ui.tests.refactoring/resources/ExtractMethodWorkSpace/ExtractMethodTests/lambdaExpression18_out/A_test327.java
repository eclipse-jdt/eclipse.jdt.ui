package lambdaExpression18_in;

import java.util.List;

public class C1 {
    void doIt() {
        List<String> list= List.of("a", "b");
        String search = "b";

        extracted(list, search);
    }

	private void extracted(List<String> list, String search) {
		/*[*/final boolean isFound = list.stream()
                .anyMatch(search::equals);/*]*/
	}
}