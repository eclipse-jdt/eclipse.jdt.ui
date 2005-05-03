package argument_out;

import java.text.MessageFormat;

public class Test91470 {
	public static final String format(String key, Object... args) {
	    return MessageFormat.format(key, args);
	}
	
	public void use() {
		/*]*/Object[] args = {"value"};
		MessageFormat.format("key", args);/*[*/
	}
}
