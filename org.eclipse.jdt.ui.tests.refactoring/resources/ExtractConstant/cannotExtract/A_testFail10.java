//15, 20 -> 15, 37   AllowLoadtime == false
package p;

class R {
	static R instance() {
		return null;
	}
	
	int rF() {
		return 1;	
	}
	
	static class S extends R {
		int f(){
			int d= R.instance().rF();		
			return 0;	
		}
	}
}