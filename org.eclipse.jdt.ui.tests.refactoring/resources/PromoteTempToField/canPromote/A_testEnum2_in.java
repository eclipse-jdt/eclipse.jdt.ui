//10, 21, 10, 21
package p;

class C {
	enum Member {
		FIRST, SECOND;
	}
	void use() {
		Member m= Member.SECOND;
		Member m2= m;
	}
}