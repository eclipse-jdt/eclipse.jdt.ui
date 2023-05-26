package p; // 11, 13, 11, 32

import java.io.File;

public class A extends File
{
	private String l;

	public A(String parent, String child) {
		super(parent, child.toLowerCase());
		String lowerCase= child.toLowerCase();
		l = lowerCase;
	}

}
