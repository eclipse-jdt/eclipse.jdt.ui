package simple18_in;

import java.lang.String;

public class TestTextBlock1 {
	public static void main(int a, int b) {
		System.out.println(/*]*/getVal()/*[*/);
	}

	private static String getVal() {
		return """
			   Sample
			   Text Block
			   """;
	}
}
