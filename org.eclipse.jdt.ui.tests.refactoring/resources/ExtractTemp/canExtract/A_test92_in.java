package p;

import static java.util.Arrays.*;

class Bug {
	final static String[] side = new String[0];
	{
		if (true) {
			System.out.println(asList(side));
		} else {
			System.out.println(asList(side));
		}
	}
}
