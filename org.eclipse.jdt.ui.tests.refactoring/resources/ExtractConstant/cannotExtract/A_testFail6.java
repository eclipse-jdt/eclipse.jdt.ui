//11, 20 -> 11, 24   AllowLoadtime == true
package p;
class R {
	int rF() {
		return 1;
	}
	
	
	static class S extends R {
		int f(){
			int t= rF();
			return t * t;
		}	
	}	
}