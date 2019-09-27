package p;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

public class Foo implements IFoo {
	@Override
	public int log(Field field, String message) {
		ManagementFactory factory = null;
		return -1;
	}
}
