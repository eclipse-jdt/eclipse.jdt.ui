package lambdaExpression18_in;

import java.util.List;

public class C1 {
    void doIt() {
        List<String> list= List.of("a", "b");
        String search = "b";

        /*[*/final boolean isFound = list.stream()
                .anyMatch(search::equals);/*]*/
    }
}