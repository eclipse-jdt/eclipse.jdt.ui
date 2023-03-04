package p; // 11, 17, 11, 36

public class A {
	char handle(final String value, int index) {
		char result = ' ';
		char charAt= value.charAt(index);
		if (charAt != ' ' && value.contains("WR")) {
			result = charAt;
			index += 2;
		} else if (index == 0 && (value.charAt(index + 1) == 'a' || value.contains("WH"))) {
			index++;
			char charAt2= value.charAt(index);
			if (charAt2 == 'a') {
				result = charAt2;
				index++;
			} else {
				result = 'A';
				index++;
			}
		} else {
			char charAt3= value.charAt(index);
			if (charAt3 == 'a' || value.contains("EWSKY") || value.contains("SCH")) {
				index++;
				result = 'F';
			} else if (charAt3 != 'a' && value.endsWith("WICZ")) {
				result = 'X';
				index += 4;
				if (index < value.length()) {
					char charAt4= value.charAt(index);
					System.out.print(charAt4);
				}
			} else {
				char charAt5= value.charAt(index);
				result = charAt5;
			}
		}
		char charAt6= value.charAt(index);
		return result > charAt6 ? charAt6 : result;
	}
}
