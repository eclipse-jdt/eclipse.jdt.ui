//13, 20 -> 13, 35   AllowLoadtime == true
package p;

class R {
	public static R instance= new R();
	
	int rF() {
		return 1;
	}
	
	static class S extends R {
		private static final int CONSTANT= R.instance.rF();

		int f(){
			int d= CONSTANT;		
			return 0;	
		}
	}
}