package p;

class Test {
  
  public static final Test FOO = new Test() {
    public void foo() {
      int var = 1;
      int var2 = var;
    }
  };
}
