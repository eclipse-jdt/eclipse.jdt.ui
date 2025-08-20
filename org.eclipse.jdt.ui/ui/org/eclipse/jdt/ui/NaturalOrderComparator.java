package org.eclipse.jdt.ui;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @since 3.36
 */
public class NaturalOrderComparator implements Comparator<String> {

    @Override
    public int compare(String s1, String s2) {

        if (s1 == null || s2 == null) {
            return (s1 == null) ? (s2 == null ? 0 : -1) : 1;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return s1.length() - s2.length();
        }

        LinkedList<String> parts1 = splitIntoDigitAndNonDigitParts(s1);
        LinkedList<String> parts2 = splitIntoDigitAndNonDigitParts(s2);

        Iterator<String> it2 = parts2.iterator();

        for (String part1 : parts1) {
            if (!it2.hasNext()) {
                return 1;
            }

            String part2 = it2.next();

            int result = compareParts(part1, part2);
            if (result != 0) {
                return result;
            }
        }

        if (it2.hasNext()) {
            return -1;
        }

        return s1.compareTo(s2);
    }

    private int compareParts(String part1, String part2) {
        boolean numeric1 = Character.isDigit(part1.charAt(0));
        boolean numeric2 = Character.isDigit(part2.charAt(0));

        if (numeric1 && numeric2) {
            String num1 = stripLeadingZeros(part1);
            String num2 = stripLeadingZeros(part2);

            int lengthDiff = num1.length() - num2.length();
            if (lengthDiff != 0) {
                return lengthDiff;
            }
            return num1.compareToIgnoreCase(num2);
        }
        return part1.compareToIgnoreCase(part2);
    }

    private LinkedList<String> splitIntoDigitAndNonDigitParts(String input) {
        LinkedList<String> parts = new LinkedList<>();
        int partStart = 0;
        boolean prevIsDigit = Character.isDigit(input.charAt(0));

        for (int i = 1; i < input.length(); i++) {
            boolean isDigit = Character.isDigit(input.charAt(i));
            if (isDigit != prevIsDigit) {
                parts.add(input.substring(partStart, i));
                partStart = i;
                prevIsDigit = isDigit;
            }
        }
        parts.add(input.substring(partStart));
        return parts;
    }

    private String stripLeadingZeros(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) != '0') {
                return input.substring(i);
            }
        }
        return ""; //$NON-NLS-1$
    }
}
