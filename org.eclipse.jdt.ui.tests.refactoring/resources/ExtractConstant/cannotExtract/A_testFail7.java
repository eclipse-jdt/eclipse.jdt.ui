//11, 20 -> 11, 34   AllowLoadtime == true
package p;

class R {
	int rF() {
		return 1;
	}
	
	static class S extends R {
		void foo() {
			int u= super.rF() + 1;	
		}
	}	
}