// 7, 13 -> 7, 24
package p;

class A {
	public boolean fState= false;
	public void foo() {
		if (this.fState) {
			this.fState= false;
		} else {
			this.fState= this.fState;
		}
	}
}