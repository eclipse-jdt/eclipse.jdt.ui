package p;

import java.util.List;

class A {
    void m(List<String> arg) {
        for (String /*[*/each/*]*/ : arg) {
            String itch= each;
        }
    }
}