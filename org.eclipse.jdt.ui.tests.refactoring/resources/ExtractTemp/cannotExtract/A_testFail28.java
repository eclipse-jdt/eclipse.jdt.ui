// 7, 17 -> 7, 28
package p;

class A {
	public int fState= 0;
	public void foo() {
		fState= this.fState;
		fState= fState;
		this.fState= this.fState;
		this.fState= fState;
		this.fState++;
		this.fState--;
		fState++;
		fState--;
		if (fState++ == 0)	return;
		if (fState-- == 0)	return;
		if (this.fState++ == 0)	return;
		if (this.fState-- == 0)	return;
	}
}