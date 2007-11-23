/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.jeview.views;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.JavaModelException;


public class JEMemberValuePair extends JEAttribute {
	
	private final JEAttribute fParent;
	private String fName; // can be null
	private IMemberValuePair fMemberValuePair; // can be null
	
	JEMemberValuePair(JEAttribute parent, String name, IMemberValuePair memberValuePair) {
		Assert.isNotNull(parent);
		fParent= parent;
		fName= name;
		fMemberValuePair= memberValuePair;
	}

	JEMemberValuePair(JEAttribute parent, IMemberValuePair memberValuePair) {
		this(parent, null, memberValuePair);
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
		
		JEMemberValuePair other= (JEMemberValuePair) obj;
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
		
		if (fMemberValuePair == null) {
			if (other.fMemberValuePair != null)
				return false;
		} else if (! fMemberValuePair.getMemberName().equals(other.fMemberValuePair.getMemberName())) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fName != null ? fName.hashCode() : 0)
				+ (fMemberValuePair != null ? fMemberValuePair.getMemberName().hashCode() : 0);
	}
	
	@Override
	public Object getWrappedObject() {
		return fMemberValuePair;
	}
	
	@Override
	public JEAttribute[] getChildren() {
		if (fMemberValuePair == null)
			return EMPTY;
		Object value= fMemberValuePair.getValue();
		return new JEAttribute[] { createMVPairValue(this, "VALUE", value)};
	}

	@Override
	public String getLabel() {
		StringBuffer sb= new StringBuffer();
		if (fName != null) {
			sb.append(fName).append(": ");
		}
		sb.append("IMemberValuePair: ");
		if (fMemberValuePair == null) {
			sb.append(fMemberValuePair);
		} else {
			sb.append(fMemberValuePair.getMemberName());
		}
		return sb.toString();
	}

	static JEAttribute createMVPairValue(JEAttribute parent, String name, Object value) {
		if ((value instanceof Object[])) {
			return createArrayValuedMVPair(parent, name, (Object[]) value);
		
		} else if (value instanceof IAnnotation) {
			return new JavaElement(parent, name, (IAnnotation) value);
		
		} else if (value != null) {
			return new JavaElementProperty(parent, name, value);
			
		} else {
			return new Null(parent, name);
		}
	}

	static JEAttribute createArrayValuedMVPair(JEAttribute parent, String name, final Object[] values) {
		return new JavaElementChildrenProperty(parent, name) {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				JEAttribute[] children= new JEAttribute[values.length];
				for (int i= 0; i < values.length; i++) {
					Object value= values[i];
					String childName= value == null ? "" : value.getClass().getSimpleName(); 
					children[i]= createMVPairValue(this, childName, value);
				}
				return children;
			}
		};
	}
}
