package p;
class Sup{
}
class B extends Sup {
	private static int CONSTANT= 0;
}

class Test {
  public static void main(String[] arguments) {
    System.out.println(B.CONSTANT);
  }
}