package simple_out;

public class TestSuper {
  private static class A {
    void foo() {
    	bar();    
    }
    void bar() {
    }
  }
  private static class B extends A {
    void foo() {
      bar();
    }
  }
}

