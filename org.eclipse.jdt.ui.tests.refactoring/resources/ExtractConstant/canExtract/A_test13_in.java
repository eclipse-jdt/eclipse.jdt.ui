//9, 16 -> 9, 28   AllowLoadtime == true
package p;

class S {
	public static S instance= new S();
	
	int f(){

		int v= instance.f();
		return 0;	
	}
}