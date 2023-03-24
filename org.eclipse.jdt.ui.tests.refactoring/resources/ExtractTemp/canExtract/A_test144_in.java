package p; // 11, 17, 11, 36

public class A {
	char handle(final String value, int index) {
		char result = ' ';
		if (value.charAt(index) != ' ' && value.contains("WR")) {
			result = value.charAt(index);
			index += 2;
		} else if (index == 0 && (value.charAt(index + 1) == 'a' || value.contains("WH"))) {
			index++;
			if (value.charAt(index) == 'a') {
				result = value.charAt(index);
				index++;
			} else {
				result = 'A';
				index++;
			}
		} else if (value.charAt(index) == 'a' || value.contains("EWSKY") || value.contains("SCH")) {
			index++;
			result = 'F';
		} else if (value.charAt(index) != 'a' && value.endsWith("WICZ")) {
			result = 'X';
			index += 4;
			if (index < value.length())
				System.out.print(value.charAt(index));
		} else {
			result = value.charAt(index);
		}
		return result > value.charAt(index) ? value.charAt(index) : result;
	}
}
