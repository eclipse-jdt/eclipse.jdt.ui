package p;

class A {
	int i;
	void m(int i, boolean bool) {this.i = i;}
}
class Super extends A {
	void m(int i, boolean bool) { super.m(1, bool); }
}
class Recursive extends A {
	void m(int i, boolean bool) { if (true) m(i, bool); }
}
class ThisRecursive extends A {
	void m(int i, boolean bool) { this.m(i, bool); }
}
class AlmostRecursive extends A {
	void m(int i, boolean bool) { new A().m(i, true); }
}
class RecursiveOrNot extends A {
	void m(int i, boolean bool) { new RecursiveOrNot().m(i, bool); }
}
class NonRecursive extends A {
	void m(int i, boolean bool) { int k= i; }
}
class Calling extends A {
	void bar() { m(17, true); }
}
class Calling2 {
	void bar() { new A().m(17, true); }
}
class Calling3 {
	void bar() { new Recursive().m(17, true); }
}
