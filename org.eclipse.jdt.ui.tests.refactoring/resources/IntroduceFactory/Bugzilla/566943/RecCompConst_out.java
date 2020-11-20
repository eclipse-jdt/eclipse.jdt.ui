package p;

import java.util.List;

public record RecCompConst(int a, char c, String b) {

	public static RecCompConst createRecCompConst(int a, char c, String b) {
		return new RecCompConst(a, c, b);
	}

	public /*[*/RecCompConst/*]*/ {
    }

}
