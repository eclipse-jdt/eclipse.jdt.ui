/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.Objects;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess;


public class CPListElementAttribute {

	private CPListElement fParent;
	private String fKey;
	private Object fValue;
	private final boolean fBuiltIn;
	private IStatus fStatus;

	private ClasspathAttributeAccess fCachedAccess;

	public CPListElementAttribute(CPListElement parent, String key, Object value, boolean builtIn) {
		fKey= key;
		fValue= value;
		fParent= parent;
		fBuiltIn= builtIn;
		if (!builtIn) {
			Assert.isTrue(value instanceof String || value == null);
		}
		fStatus= getContainerChildStatus();
	}

    private CPListElementAttribute(boolean buildIn) {
    	fBuiltIn= buildIn;
    }

	public IClasspathAttribute getClasspathAttribute() {
		Assert.isTrue(!fBuiltIn);
		return JavaCore.newClasspathAttribute(fKey, (String) fValue);
	}

	public CPListElement getParent() {
		return fParent;
	}

	/**
	 * @return Returns <code>true</code> if the attribute is a built in attribute.
	 */
	public boolean isBuiltIn() {
		return fBuiltIn;
	}

	/**
	 * @return Returns <code>true</code> if the attribute a on a container child and is read-only
	 */
	public boolean isNonModifiable() {
		if (fStatus != null && !fStatus.isOK()) {
			return true;
		}
		if (fBuiltIn) {
			if (IClasspathAttribute.MODULE.equals(this.fKey)) {
				IJavaProject javaProject= fParent.getJavaProject();
				if (!JavaModelUtil.is9OrHigher(javaProject)) {
					return true; // cannot set "module=true" in 1.8- project
				}
			}
		}
		return false;
	}

	/**
	 * @return Returns <code>true</code> if the attribute a on a container child and is not supported
	 */
	public boolean isNotSupported() {
		return fStatus != null && fStatus.getCode() == ClasspathContainerInitializer.ATTRIBUTE_NOT_SUPPORTED;
	}

	/**
	 * @return Returns the container child status or <code>null</code> if the attribute is not in a container child
	 */
	private IStatus getContainerChildStatus() {
		return fParent.getContainerChildStatus(this);
	}


	/**
	 * Returns the key.
	 * @return String
	 */
	public String getKey() {
		return fKey;
	}

	/**
	 * Returns the value.
	 * @return Object
	 */
	public Object getValue() {
		return fValue;
	}

	/**
	 * Returns the value.
	 * @param value value to set
	 */
	public void setValue(Object value) {
		fValue= value;
		fCachedAccess= null;
		getParent().attributeChanged(fKey);
	}

    @Override
	public boolean equals(Object obj) {
        if (!(obj instanceof CPListElementAttribute))
            return false;
        CPListElementAttribute attrib= (CPListElementAttribute)obj;
        return Objects.equals(attrib.fKey, this.fKey)
        		&& Objects.equals(attrib.getParent().getPath(), fParent.getPath());
    }

    @Override
	public int hashCode() {
    	return fKey.hashCode() * 89 + fParent.getPath().hashCode();
    }

    public CPListElementAttribute copy() {
    	CPListElementAttribute result= new CPListElementAttribute(fBuiltIn);
    	result.fParent= fParent;
    	result.fKey= fKey;
    	result.fValue= fValue;
    	result.fStatus= fStatus;
	    return result;
    }

    public ClasspathAttributeAccess getClasspathAttributeAccess() {
    	if (fCachedAccess == null) {
	    	fCachedAccess= new ClasspathAttributeAccess() {
	    		@Override
				public IClasspathAttribute getClasspathAttribute() {
	 				return CPListElementAttribute.this.getClasspathAttribute();
				}
				@Override
				public IJavaProject getJavaProject() {
					return getParent().getJavaProject();
				}
				@Override
				public IClasspathEntry getParentClasspassEntry() {
					return getParent().getClasspathEntry();
				}
	    	};
    	}
    	return fCachedAccess;
    }


}
