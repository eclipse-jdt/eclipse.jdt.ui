/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.Comparator;

/**
 * Compares two Java editor hover descriptors. The
 * relationship is built based upon their dependency.
 * 
 * @since 3.0
 */
public class JavaEditorTextHoverDescriptorComparator implements Comparator {
	
	/*
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object object0, Object object1) {

		JavaEditorTextHoverDescriptor element0= (JavaEditorTextHoverDescriptor)object0;
		JavaEditorTextHoverDescriptor element1= (JavaEditorTextHoverDescriptor)object1;	

		String id0=	element0.getId();
		String id1= element1.getId();
		
		if (id0 != null && id0.equals(id1))
			return 0;
		
		if (id0 != null && AnnotationHover.isJavaProblemHover(id0))
			return -1;

		if (id1 != null && AnnotationHover.isJavaProblemHover(id1))
			return +1;


		// now compare non-problem hovers
		if (element0.dependsOn(element1))
			return -1;

		if (element1.dependsOn(element0))
			return +1;
		
		return 0;
	}
}