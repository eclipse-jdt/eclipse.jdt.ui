//rename var to: j
package p;
abstract class Test {
  public static final Test FOO = new Test() {
    public void foo() {
      int var = 1;
    }
  };
}