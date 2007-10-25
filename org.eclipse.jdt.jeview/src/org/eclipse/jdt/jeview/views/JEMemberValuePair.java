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
	private IMemberValuePair fMemberValuePair;
	
	JEMemberValuePair(JEAttribute parent, IMemberValuePair memberValuePair) {
		Assert.isNotNull(parent);
		Assert.isNotNull(memberValuePair);
		fParent= parent;
		fMemberValuePair= memberValuePair;
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
		
		if (! fMemberValuePair.getMemberName().equals(other.fMemberValuePair.getMemberName())) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ fMemberValuePair.getMemberName().hashCode();
	}
	
	@Override
	public Object getWrappedObject() {
		return fMemberValuePair;
	}
	
	@Override
	public JEAttribute[] getChildren() {
		Object value= fMemberValuePair.getValue();
		return new JEAttribute[] { createMVPairValue(this, "VALUE", value)};
	}

	@Override
	public String getLabel() {
		return "IMemberValuePair: " + fMemberValuePair.getMemberName();
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
