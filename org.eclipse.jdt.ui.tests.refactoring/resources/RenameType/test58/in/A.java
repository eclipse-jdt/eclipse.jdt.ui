package p;
class Sup{
}
class A extends Sup {
	private static int CONSTANT= 0;
}

class Test {
  public static void main(String[] arguments) {
    System.out.println(A.CONSTANT);
  }
}