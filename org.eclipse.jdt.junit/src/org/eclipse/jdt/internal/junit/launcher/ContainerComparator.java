package org.eclipse.jdt.internal.junit.launcher;

import java.util.Comparator;

public class ContainerComparator implements Comparator {

    public int compare(Object o1, Object o2) {
		String container1= (String)o1;
		String container2= (String)o2;
		if (container1 == null)
			container1= "";
		if (container2 == null)
			container2= "";
		return container1.compareTo(container2);
    }
}
