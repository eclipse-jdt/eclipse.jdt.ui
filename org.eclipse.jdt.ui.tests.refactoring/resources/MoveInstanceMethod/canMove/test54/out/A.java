package p;

public class A {
  public long m(B b, int i) {
	return b.m(i);
}
}
public class B extends A {
  public long test() {
    return super.m(null, 2);
  }

public long m(int i) {
  	return 1;
  }
}