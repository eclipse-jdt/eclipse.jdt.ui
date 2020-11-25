package p;

import java.util.*;

public record A(int a, char b) {
	record B(int c, String d) {
		int newName() {
			return 0;
		}
	}
}