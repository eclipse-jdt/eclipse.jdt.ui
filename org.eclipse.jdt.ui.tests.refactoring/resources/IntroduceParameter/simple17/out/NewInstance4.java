//selection: 10, 43, 10, 68
//name: hashMap -> hashMap
package simple;

import java.util.Map;
public class Diamond {
    class HashMap {}

    void foo(java.util.HashMap<String, Number> hashMap) {
        Map<String, ? extends Number> m = hashMap;
    }

    public static void main(String[] args) {
        new Diamond().foo(new java.util.HashMap<String, Number>());
    }
}
