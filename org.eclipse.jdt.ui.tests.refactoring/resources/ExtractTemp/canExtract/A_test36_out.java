package p;


class Test {

  public int[] foo() {
    return null;
  }  
  
  public void bar(Test test) {
    int[] temp= test.foo();
	int[] i = temp; // refactor this
  }

}