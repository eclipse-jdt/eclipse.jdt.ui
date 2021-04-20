/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

package org.eclipse.jdt.jeview.properties;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import org.eclipse.jdt.core.IMemberValuePair;

public class MemberValuePairProperties implements IPropertySource {

	private static final String C_MEMBER_VALUE_PAIR= "IMemberValuePair";

	private static final String P_MEMBER_NAME= "org.eclipse.jdt.jeview.IMemberValuePair.memberName";
	private static final String P_VALUE_KIND= "org.eclipse.jdt.jeview.IMemberValuePair.valueKind";

	protected IMemberValuePair fMemberValuePair;

	private static final ArrayList<IPropertyDescriptor> MEMBER_VALUE_PAIR_PROPERTY_DESCRIPTORS= new ArrayList<>();
	static {
		addResourceDescriptor(new PropertyDescriptor(P_MEMBER_NAME, "memberName"));
		addResourceDescriptor(new PropertyDescriptor(P_VALUE_KIND, "valueKind"));
	}

	private static void addResourceDescriptor(PropertyDescriptor descriptor) {
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(C_MEMBER_VALUE_PAIR);
		MEMBER_VALUE_PAIR_PROPERTY_DESCRIPTORS.add(descriptor);
	}


	public MemberValuePairProperties(IMemberValuePair memberValuePair) {
		fMemberValuePair= memberValuePair;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		ArrayList<IPropertyDescriptor> result= new ArrayList<>(MEMBER_VALUE_PAIR_PROPERTY_DESCRIPTORS);
		return result.toArray(new IPropertyDescriptor[result.size()]);
	}

	@Override
	public Object getPropertyValue(Object name) {
		if (P_MEMBER_NAME.equals(name)) {
			return fMemberValuePair.getMemberName();
		} else 	if (P_VALUE_KIND.equals(name)) {
			return getValueKindName(fMemberValuePair.getValueKind());
		}

		return null;
	}

	@Override
	public void setPropertyValue(Object name, Object value) {
		// do nothing
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public boolean isPropertySet(Object property) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object property) {
		// do nothing
	}

	static String getValueKindName(int valueKind) {
		String name= "UNKNOWN";
		Field[] fields= IMemberValuePair.class.getFields();
		for (Field f : fields) {
			try {
				if (f.getType() == int.class && f.getInt(f) == valueKind) {
					name= "IMemberValuePair." + f.getName();
					break;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// continue
			}
		}
		return valueKind + " (" + name + ")";
	}

}
