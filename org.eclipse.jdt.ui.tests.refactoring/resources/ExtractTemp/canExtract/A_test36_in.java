package p;


class Test {

  public int[] foo() {
    return null;
  }  
  
  public void bar(Test test) {
    int[] i = test.foo(); // refactor this
  }

}