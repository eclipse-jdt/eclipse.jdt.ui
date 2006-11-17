package p;

import static java.util.Arrays.*;

import java.util.List;

class Bug {
	final static String[] side = new String[0];
	private static final List<String> AS_LIST= asList(side);
	{
		if (true) {
			System.out.println(AS_LIST);
		} else {
			System.out.println(AS_LIST);
		}
	}
}
