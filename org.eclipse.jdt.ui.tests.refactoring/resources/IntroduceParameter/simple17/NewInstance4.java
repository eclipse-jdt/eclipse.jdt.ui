//selection: 10, 43, 10, 68
//name: hashMap -> hashMap
package simple;

import java.util.Map;
public class Diamond {
    class HashMap {}

    void foo() {
        Map<String, ? extends Number> m = new java.util.HashMap<>();
    }

    public static void main(String[] args) {
        new Diamond().foo();
    }
}
