package bugs_out;

class Test_394725 {

    private static class C {
    }

    void f() {
        Object o = C.class;
    }
}
