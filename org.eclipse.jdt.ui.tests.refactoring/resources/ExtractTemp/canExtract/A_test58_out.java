// 7, 13 -> 7, 24
package p;

class A {
	public boolean fState= false;
	public void foo() {
		boolean temp= this.fState;
		if (temp) {
			this.fState= false;
		} else {
			this.fState= temp;
		}
	}
}