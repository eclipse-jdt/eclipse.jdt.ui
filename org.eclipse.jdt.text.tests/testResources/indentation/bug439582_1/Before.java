package indentbug;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Bug1 {
	static final Set<Object> x = new HashSet<Object>(Arrays.asList(new Color[] {
			Color.RED,
		Color.GREEN,
			Color.BLUE
	}));	
}

enum Color {
	RED, GREEN, BLUE
}
