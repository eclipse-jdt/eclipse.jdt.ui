//9, 16 -> 9, 28   AllowLoadtime == true
package p;

class S {
	public static S instance= new S();
	private static final int CONSTANT= instance.f();
	
	int f(){

		int v= CONSTANT;
		return 0;	
	}
}