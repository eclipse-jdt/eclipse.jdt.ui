//selection: 8, 17, 8, 18
package invalid;

class NoMethodBinding {
	void method() { }
	
	void method() {
		int x = 3; //<-- introduce 3 as a parameter
	}
}