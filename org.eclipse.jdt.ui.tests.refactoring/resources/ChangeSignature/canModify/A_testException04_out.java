package p;

import java.io.IOException;

abstract class A {
	public abstract void m() throws IOException, ClassNotFoundException;
}

class B extends A {
	public int m() throws RuntimeException, IOException, ClassNotFoundException {
	}
}