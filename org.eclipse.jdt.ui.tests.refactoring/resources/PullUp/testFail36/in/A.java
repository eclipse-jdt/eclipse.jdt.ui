package p;

public class A {
    public class BaseTargetClass {}

    public class OriginalClass {
        public int data = 60;
        public void memberMethod() {}

        public class NestedOriginalClass extends BaseTargetClass {
            void setup() {
                new BaseTargetClass() {
                    void method() {
                        methodHelper();
                    }

                    void methodHelper() {
                        System.out.println("Helper Method in Anonymous Class: " + data);
                    }
                };
            }
        }
    }
}
