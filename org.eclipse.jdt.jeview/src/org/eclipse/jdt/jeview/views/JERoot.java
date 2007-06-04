/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;


public class JERoot extends JEAttribute {

	private final List<JEAttribute> fJEAttributes;

	public JERoot(Collection<?> javaElementsOrResources) {
		fJEAttributes= new Mapper<Object, JEAttribute>() {
			@Override public JEAttribute map(Object element) {
				if (element instanceof IJavaElement)
					return new JavaElement(null, (IJavaElement) element);
				else if (element instanceof IResource)
					return new JEResource(null, null, (IResource) element);
				else if (element instanceof IJarEntryResource)
					return new JEJarEntryResource(null, null, (IJarEntryResource) element);
				else
					throw new IllegalArgumentException(String.valueOf(element));
				
			}
		}.mapToList(javaElementsOrResources);
		
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
		return fJEAttributes.toArray(new JEAttribute[fJEAttributes.size()]);
	}

	@Override
	public Object getWrappedObject() {
		return null;
	}
	
	@Override
	public String getLabel() {
		StringBuffer buf = new StringBuffer("root: ");
		boolean first= true;
		for (JEAttribute att : fJEAttributes) {
			if (! first)
				buf.append(", ");
			buf.append(att.getLabel());
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
		return fJEAttributes.equals(other.fJEAttributes);
	}
	
	@Override
	public int hashCode() {
		return fJEAttributes.hashCode();
	}

}
