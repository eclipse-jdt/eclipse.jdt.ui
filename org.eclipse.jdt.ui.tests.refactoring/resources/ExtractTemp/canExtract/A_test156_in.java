package p; // 11, 13, 11, 32

import java.io.File;

public class A extends File
{
	private String l;

	public A(String parent, String child) {
		this(parent, child.toLowerCase(), null);
		l = child.toLowerCase();
	}

	public A(String parent, String child, Object k) {
		super(parent, child);
	}

}
