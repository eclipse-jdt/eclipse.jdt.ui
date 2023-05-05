package bugs_in;

class Test_394725 {

    private static class C {
        static void /*]*/f/*[*/() {
            Object o = C.class;
        }
    }

    void f() {
        C.f();
    }
}
