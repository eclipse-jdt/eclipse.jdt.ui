package p;

import java.io.IOException;
import java.util.zip.ZipException;

abstract class A {
	public abstract int m(long l) throws IOException, ZipException;
}

class B extends A {
	public int m(long l) throws IOException, ZipException {
		return 17;
	}
	public int m(int i) {
		return i;
	}
}

class C extends B {
}

class D extends A {
	public int m(long l) throws ZipException {
		return 0;
	}
}	