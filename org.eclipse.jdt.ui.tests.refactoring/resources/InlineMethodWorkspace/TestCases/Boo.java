public class Boo {
  private static class A {
    void foo() {    
    }
  }
  private static class B extends A {
    void foo() {
      super.foo();
    }
  }
}

