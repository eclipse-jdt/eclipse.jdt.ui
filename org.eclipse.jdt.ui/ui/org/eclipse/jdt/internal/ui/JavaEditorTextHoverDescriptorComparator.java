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
package org.eclipse.jdt.internal.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginPrerequisite;

import org.eclipse.jface.text.Assert;

import org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverDescriptor;

/**
 * Compares two Java editor hover descriptors. The
 * relationship is built based upon their dependency.
 * <p>
 * Note: This comarator is only valid for comparing
 * the elements of the given Java editor descriptors.
 * </p>
 * 
 * @since 3.0
 */
class JavaEditorTextHoverDescriptorComparator implements Comparator {
	
	private Map fDescriptorMapping;
	private Set fDescriptorSet;
	private Map fPrereqsMapping;

	public JavaEditorTextHoverDescriptorComparator(JavaEditorTextHoverDescriptor[] elements) {
		Assert.isNotNull(elements);
		initialize(elements);
	}
	
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
		
		// now compare non-problem hovers
		if (dependsOn(element0, element1))
			return -1;

		if (dependsOn(element1, element0))
			return +1;
		
		return 0;
	}

	/**
	 * Returns whether one configuration element depends on the other element.
	 * This is done by checking the dependency chain of the defining plug-ins.
	 * 
	 * @param descriptor a JavaEditorTextHoverDescriptor
	 * @return <code>true</code> if this contributed hover depends on the other one
	 */
	private boolean dependsOn(JavaEditorTextHoverDescriptor descriptor0, JavaEditorTextHoverDescriptor descriptor1) {
		if (descriptor1 == null || descriptor0 == null)
			return false;
		
		IPluginDescriptor pluginDesc0= (IPluginDescriptor)fDescriptorMapping.get(descriptor0);
		IPluginDescriptor pluginDesc1= (IPluginDescriptor)fDescriptorMapping.get(descriptor1);
		
		// performance tuning - code below would give same result
		if (pluginDesc0.getUniqueIdentifier().equals(pluginDesc1.getUniqueIdentifier()))
			return false;
		
		
		Set prereqUIds0= (Set)fPrereqsMapping.get(pluginDesc0);
		
		return prereqUIds0.contains(pluginDesc1.getUniqueIdentifier());
	}

	/**
	 * Initialize this comarator.
	 * 
	 * @param elements an array of Java editor hover descriptors
	 */
	private void initialize(JavaEditorTextHoverDescriptor[] elements) {
		int length= elements.length;
		fDescriptorMapping= new HashMap(length);
		fPrereqsMapping= new HashMap(length);
		fDescriptorSet= new HashSet(length);
		
		for (int i= 0; i < length; i++) {
			IPluginDescriptor descriptor= elements[i].getConfigurationElement().getDeclaringExtension().getDeclaringPluginDescriptor();
			fDescriptorMapping.put(elements[i], descriptor);
			fDescriptorSet.add(descriptor);
		}
		
		Iterator iter= fDescriptorSet.iterator();
		while (iter.hasNext()) {
			IPluginDescriptor descriptor= (IPluginDescriptor)iter.next();
			List toTest= new ArrayList(fDescriptorSet);
			toTest.remove(descriptor);
			Set prereqUIds= new HashSet(Math.max(0, toTest.size() - 1));
			fPrereqsMapping.put(descriptor, prereqUIds);
			
			IPluginPrerequisite[] prereqs= descriptor.getPluginPrerequisites();
			int i= 0;
			while (i < prereqs.length && !toTest.isEmpty()) {
				String prereqUId= prereqs[i].getUniqueIdentifier();
				for (int j= 0; j < toTest.size();) {
					IPluginDescriptor toTest_j= (IPluginDescriptor)toTest.get(j);
					if (toTest_j.getUniqueIdentifier().equals(prereqUId)) {
						toTest.remove(toTest_j);
						prereqUIds.add(toTest_j.getUniqueIdentifier());
					} else
						j++;
				}
				i++;
			}
		}
	}
}