/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.text.java.hover;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginPrerequisite;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.ITextViewerExtension2;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Describes a Java editor text hover.
 * 
 * @since 2.1
 */
public class JavaEditorTextHoverDescriptor implements Comparable {

	private static final String JAVA_EDITOR_TEXT_HOVER_EXTENSION_POINT= "org.eclipse.jdt.ui.javaEditorTextHovers"; //$NON-NLS-1$
	private static final String HOVER_TAG= "hover"; //$NON-NLS-1$
	private static final String ID_ATTRIBUTE= "id"; //$NON-NLS-1$
	private static final String CLASS_ATTRIBUTE= "class"; //$NON-NLS-1$
	private static final String LABEL_ATTRIBUTE= "label"; //$NON-NLS-1$
	private static final String ACTIVATE_PLUG_IN_ATTRIBUTE= "label"; //$NON-NLS-1$
	
	private static List fgContributedHovers;
	

	private IConfigurationElement fElement;

	/**
	 * Returns all Java editor text hovers contributed to the workbench.
	 */
	public static List getContributedHovers() {
		if (fgContributedHovers == null) {
			IPluginRegistry registry= Platform.getPluginRegistry();
			IConfigurationElement[] elements= registry.getConfigurationElementsFor(JAVA_EDITOR_TEXT_HOVER_EXTENSION_POINT);
			fgContributedHovers= createDescriptors(elements);
		}
		return fgContributedHovers;
	} 

	/**
	 * Returns the id of the configured default hover.	 */
	public static String getDefaultHoverId() {
		return JavaPlugin.getDefault().getPreferenceStore().getString(JavaEditor.DEFAULT_HOVER);
	}

	public static JavaEditorTextHoverDescriptor getTextHoverDescriptor(int stateMask) {
		String key= null;
		switch (stateMask) {
			case ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK:
				key= JavaEditor.DEFAULT_HOVER;
				break;
			case SWT.NONE:
				key= JavaEditor.NONE_HOVER;
				break;
			case SWT.CTRL:
				key= JavaEditor.CTRL_HOVER;
				break;
			case SWT.SHIFT:
				key= JavaEditor.SHIFT_HOVER;
				break;
			case SWT.CTRL | SWT.ALT:
				key= JavaEditor.CTRL_ALT_HOVER;
				break;
			case SWT.CTRL | SWT.SHIFT:
				key= JavaEditor.CTRL_SHIFT_HOVER;
				break;
			case SWT.CTRL | SWT.ALT | SWT.SHIFT:
				key= JavaEditor.CTRL_ALT_SHIFT_HOVER;
				break;
			case SWT.ALT | SWT.SHIFT:
				key= JavaEditor.ALT_SHIFT_HOVER;
				break;
			default :
				return null;
		}
		String id= JavaPlugin.getDefault().getPreferenceStore().getString(key);
		if (JavaEditor.DEFAULT_HOVER_CONFIGURED_ID.equals(id))
			id= JavaPlugin.getDefault().getPreferenceStore().getString(JavaEditor.DEFAULT_HOVER);
		if (id == null)
			return null;

		Iterator iter= getContributedHovers().iterator();
		while (iter.hasNext()) {
			JavaEditorTextHoverDescriptor hoverDescriptor= (JavaEditorTextHoverDescriptor)iter.next();
			if (id.equals(hoverDescriptor.getId()))
				return hoverDescriptor;
		}
		return null;
	}

	/**
	 * Creates a new Java Editor text hover descriptor from the given configuration element.
	 */
	private JavaEditorTextHoverDescriptor(IConfigurationElement element) {
		Assert.isNotNull(element);
		fElement= element;
	}

	/**
	 * Creates the Java editor text hover.
	 */
	public IJavaEditorTextHover createTextHover() {
		boolean isHoversPlugInActivated= fElement.getDeclaringExtension().getDeclaringPluginDescriptor().isPluginActivated();
		if (isHoversPlugInActivated || canActivatePlugIn()) {
			try {
				return (IJavaEditorTextHover)fElement.createExecutableExtension(CLASS_ATTRIBUTE);
			} catch (CoreException x) {
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, JavaUIMessages.getString("JavaTextHover.createTextHover"), null)); //$NON-NLS-1$
			}
		}
		
		return null;
	}
	
	//---- XML Attribute accessors ---------------------------------------------
	
	/**
	 * Returns the hover's id.
	 */
	public String getId() {
			return fElement.getAttribute(ID_ATTRIBUTE);
	}

	/**
	 * Returns the hover's class name.
	 */
	public String getHoverClassName() {
		return fElement.getAttribute(CLASS_ATTRIBUTE);
	}
	 
	/**
	 * Returns the hover's label.
	 */
	public String getLabel() {
		String label= fElement.getAttribute(LABEL_ATTRIBUTE);
		if (label != null)
			return label;
			
		// Return simple class name
		label= getHoverClassName();
		int lastDot= label.lastIndexOf('.');
		if (lastDot >= 0 && lastDot < label.length() - 1)
			return label.substring(lastDot + 1);
		else
			return label;
	}

	public boolean canActivatePlugIn() {
		return Boolean.valueOf(fElement.getAttribute(ACTIVATE_PLUG_IN_ATTRIBUTE)).booleanValue();
	}

	public boolean isDefaultTextHoverDescriptor() {
		return getId().equals(getDefaultHoverId());
	}

	public boolean equals(Object obj) {
		if (obj == null || !obj.getClass().equals(this.getClass()) || getId() == null)
			return false;
		return getId().equals(((JavaEditorTextHoverDescriptor)obj).getId());
	}

	public int hashCode() {
		return getId().hashCode();
	}

	/* 
	 * Implements a method from IComparable 
	 */ 
	public int compareTo(Object o) {
		return Collator.getInstance().compare(getLabel(), ((JavaEditorTextHoverDescriptor)o).getLabel());
	}

	/**
	 * @param descriptor a JavaEditorTextHoverDescriptor
	 * @return <code>true</code> if this contributed hover depends on the other one
	 */
	public boolean dependsOn(JavaEditorTextHoverDescriptor descriptor) {
		if (descriptor == null)
			return false;
		
		IPluginDescriptor thisPluginDescriptor= fElement.getDeclaringExtension().getDeclaringPluginDescriptor();
		IPluginDescriptor otherPluginDescriptor= descriptor.fElement.getDeclaringExtension().getDeclaringPluginDescriptor();
		return dependsOn(thisPluginDescriptor, otherPluginDescriptor);
	}

	private boolean dependsOn(IPluginDescriptor descriptor0, IPluginDescriptor descriptor1) {

		IPluginRegistry registry= Platform.getPluginRegistry();
		IPluginPrerequisite[] prerequisites= descriptor0.getPluginPrerequisites();

		for (int i= 0; i < prerequisites.length; i++) {
			IPluginPrerequisite prerequisite= prerequisites[i];
			String id= prerequisite.getUniqueIdentifier();			
			IPluginDescriptor descriptor= registry.getPluginDescriptor(id);
			
			if (descriptor != null && (descriptor.equals(descriptor1) || dependsOn(descriptor, descriptor1)))
				return true;
		}
		
		return false;
	}

	private static List createDescriptors(IConfigurationElement[] elements) {
		List result= new ArrayList(5);
		for (int i= 0; i < elements.length; i++) {
			IConfigurationElement element= elements[i];
			if (HOVER_TAG.equals(element.getName())) {
				JavaEditorTextHoverDescriptor desc= new JavaEditorTextHoverDescriptor(element);
				result.add(desc);
			}
		}
		Collections.sort(result);
		return result;
	}
}