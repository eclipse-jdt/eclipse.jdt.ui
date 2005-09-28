/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.jeview.views;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;


public class JERoot extends JEAttribute {

	private final List<JavaElement> fJavaElements;

	public JERoot(Collection<? extends IJavaElement> javaElements) {
		fJavaElements= new Mapper<IJavaElement, JavaElement>() {
			@Override public JavaElement map(IJavaElement element) {
				return new JavaElement(null, element);
			}
		}.mapToList(javaElements);
		
//		fJavaElements= Mapper.build(javaElements, new Mapper<IJavaElement, JavaElement>() {
//			@Override public JavaElement map(IJavaElement element) {
//				return new JavaElement(null, element);
//			}
//		});
		
//		fJavaElements= new ArrayList<JavaElement>(javaElements.size());
//		for (IJavaElement javaElement : javaElements) {
//			fJavaElements.add(new JavaElement(null, javaElement));
//		}
	}

	@Override
	public JEAttribute getParent() {
		return null;
	}

	@Override
	public JEAttribute[] getChildren() {
		return fJavaElements.toArray(new JavaElement[fJavaElements.size()]);
	}

	@Override
	public String getLabel() {
		StringBuffer buf = new StringBuffer("root: ");
		boolean first= true;
		for (JavaElement je : fJavaElements) {
			if (! first)
				buf.append(", ");
			buf.append(je.getLabel());
			first= false;
		}
		return buf.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		JERoot other= (JERoot) obj;
		return fJavaElements.equals(other.fJavaElements);
	}
	
	@Override
	public int hashCode() {
		return fJavaElements.hashCode();
	}

}
