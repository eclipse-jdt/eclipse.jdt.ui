//9, 18 -> 9, 25   AllowLoadtime == true
package p;

class R {
	int r;

	static class S extends R {
		String f() {
			fish(super.r);
			return null;	
		}
		static void fish(int parm) {
		}
	}	
}