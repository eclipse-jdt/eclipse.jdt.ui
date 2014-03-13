package p;

public class A {
  public long m(B b, int i) {
  	return 1;
  }
}
public class B extends A {
  public long test() {
    return super.m(null, 2);
  }
}