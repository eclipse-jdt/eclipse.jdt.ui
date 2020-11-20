package p;

import java.util.List;

public record RecCanConst(int a, char c, String b) {

	public static RecCanConst createRecCanConst(int a, char c, String b) {
		return new RecCanConst(a, c, b);
	}

	public /*[*/RecCanConst/*]*/(int a, char c, String b) {
    }

}
