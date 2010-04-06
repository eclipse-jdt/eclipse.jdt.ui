/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.jeview.views;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


public class JEClasspathEntry extends JEAttribute {
	
	private final JEAttribute fParent; // can be null
	private final String fName; // can be null
	final IClasspathEntry fEntry;
	
	JEClasspathEntry(JEAttribute parent, String name, IClasspathEntry entry) {
		Assert.isNotNull(entry);
		fParent= parent;
		fName= name;
		fEntry= entry;
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		JEClasspathEntry other= (JEClasspathEntry) obj;
		if (fParent == null) {
			if (other.fParent != null)
				return false;
		} else if (! fParent.equals(other.fParent)) {
			return false;
		}
		
		if (fName == null) {
			if (other.fName != null)
				return false;
		} else if (! fName.equals(other.fName)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fName != null ? fName.hashCode() : 0)
				+ fEntry.hashCode();
	}
	
	@Override
	public Object getWrappedObject() {
		return fEntry;
	}
	
	@Override
	public JEAttribute[] getChildren() {
		ArrayList<JEAttribute> result= new ArrayList<JEAttribute>();
		
		result.add(new JavaElementChildrenProperty(this, "ACCESS RULES") {
			@Override protected JEAttribute[] computeChildren() throws CoreException {
				IAccessRule[] accessRules= fEntry.getAccessRules();
				JEAttribute[] children= new JEAttribute[accessRules.length];
				for (int i= 0; i < accessRules.length; i++) {
					children[i]= new JavaElementProperty(this, null, accessRules[i]);
				}
				return children;
			}
		});
		result.add(new JavaElementChildrenProperty(this, "EXCLUSION PATTERNS") {
			@Override protected JEAttribute[] computeChildren() throws CoreException {
				IPath[] exclusionPatterns= fEntry.getExclusionPatterns();
				JEAttribute[] children= new JEAttribute[exclusionPatterns.length];
				for (int i= 0; i < exclusionPatterns.length; i++) {
					children[i]= new JavaElementProperty(this, null, exclusionPatterns[i]);
				}
				return children;
			}
		});
		result.add(new JavaElementChildrenProperty(this, "INCLUSION PATTERNS") {
			@Override protected JEAttribute[] computeChildren() throws CoreException {
				IPath[] inclusionPatterns= fEntry.getInclusionPatterns();
				JEAttribute[] children= new JEAttribute[inclusionPatterns.length];
				for (int i= 0; i < inclusionPatterns.length; i++) {
					children[i]= new JavaElementProperty(this, null, inclusionPatterns[i]);
				}
				return children;
			}
		});
		result.add(new JavaElementChildrenProperty(this, "EXTRA ATTRIBUTES") {
			@Override protected JEAttribute[] computeChildren() throws CoreException {
				IClasspathAttribute[] extraAttributes= fEntry.getExtraAttributes();
				JEAttribute[] children= new JEAttribute[extraAttributes.length];
				for (int i= 0; i < extraAttributes.length; i++) {
					children[i]= new JavaElementProperty(this, null, extraAttributes[i]);
				}
				return children;
			}
		});
		result.add(create(this, "REFERENCING ENTRY", fEntry.getReferencingEntry()));
		result.add(new JavaElementChildrenProperty(this, "JavaCore.getReferencedClasspathEntries(this)") {
			@Override protected JEAttribute[] computeChildren() throws CoreException {
				IJavaProject project= null;
				JEAttribute parent= JEClasspathEntry.this;
				while ((parent= parent.getParent()) != null) {
					if (parent instanceof JavaElement) {
						project= ((JavaElement) parent).getJavaElement().getJavaProject();
						break;
					}
				}
				IClasspathEntry[] referencedEntries= JavaCore.getReferencedClasspathEntries(fEntry, project);
				JEAttribute[] children= new JEAttribute[referencedEntries.length];
				for (int i= 0; i < referencedEntries.length; i++) {
					children[i]= new JEClasspathEntry(this, null, referencedEntries[i]);
				}
				return children;
			}
		});
		result.add(new JEClasspathEntry(this, "JavaCore.getResolvedClasspathEntry(this)", JavaCore.getResolvedClasspathEntry(fEntry)));
		result.add(new JavaElementChildrenProperty(this, "JavaCore.getClasspathContainer(..).getClasspathEntries()") {
			@Override protected JEAttribute[] computeChildren() throws CoreException {
				IJavaProject project= null;
				JEAttribute parent= JEClasspathEntry.this;
				while ((parent= parent.getParent()) != null) {
					if (parent instanceof JavaElement) {
						project= ((JavaElement) parent).getJavaElement().getJavaProject();
						break;
					}
				}
				IClasspathContainer classpathContainer= JavaCore.getClasspathContainer(fEntry.getPath(), project);
				if (classpathContainer == null)
					return EMPTY;
				IClasspathEntry[] referencedEntries= classpathContainer.getClasspathEntries();
				JEAttribute[] children= new JEAttribute[referencedEntries.length];
				for (int i= 0; i < referencedEntries.length; i++) {
					children[i]= new JEClasspathEntry(this, null, referencedEntries[i]);
				}
				return children;
			}
		});
		
		return result.toArray(new JEAttribute[result.size()]);
	}

	@Override
	public String getLabel() {
		String label= fEntry.toString();
		if (fName != null)
			label= fName +  ": " + label;
		return label;
	}
	
	public static JEAttribute compute(JEAttribute parent, String name, Callable<IClasspathEntry> computer) {
		try {
			IClasspathEntry entry= computer.call();
			return create(parent, name, entry);
		} catch (Exception e) {
			return new Error(parent, name, e);
		}
	}

	public static JEAttribute create(JEAttribute parent, String name, IClasspathEntry entry) {
		if (entry == null) {
			return new Null(parent, name);
		} else {
			return new JEClasspathEntry(parent, name, entry);
		}
	}
	
}
