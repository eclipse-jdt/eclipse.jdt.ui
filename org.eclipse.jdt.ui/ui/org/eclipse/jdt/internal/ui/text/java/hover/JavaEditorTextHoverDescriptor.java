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
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginPrerequisite;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.Assert;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.ui.JavaPlugin;

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
	private static final String ACTIVATE_PLUG_IN_ATTRIBUTE= "activate"; //$NON-NLS-1$
	private static final String DESCRIPTION_ATTRIBUTE= "description"; //$NON-NLS-1$

	private static List fgContributedHovers;

	public static final String NO_MODIFIER= "0"; //$NON-NLS-1$
	public static final String DISABLED_TAG= "!"; //$NON-NLS-1$
	public static final String VALUE_SEPARATOR= ";"; //$NON-NLS-1$

	private int fStateMask;
	private String fModifierString;
	private boolean fIsEnabled;

	private IConfigurationElement fElement;
	
	
	/**
	 * Returns all Java editor text hovers contributed to the workbench.
	 */
	public static JavaEditorTextHoverDescriptor[] getContributedHovers() {
		IPluginRegistry registry= Platform.getPluginRegistry();
		IConfigurationElement[] elements= registry.getConfigurationElementsFor(JAVA_EDITOR_TEXT_HOVER_EXTENSION_POINT);
		JavaEditorTextHoverDescriptor[] hoverDescs= createDescriptors(elements);
		initializeFromPreferences(hoverDescs);
		return hoverDescs;
	} 

	/**
	 * Computes the state mask for the given modifier string.
	 * 
	 * @param modifiers	the string with the modifiers, separated by '+', '-', ';', ',' or '.'
	 * @return the state mask or -1 if the input is invalid
	 */
	public static int computeStateMask(String modifiers) {
		if (modifiers == null)
			return -1;
		
		if (modifiers.length() == 0)
			return SWT.NONE;

		int stateMask= 0;
		StringTokenizer modifierTokenizer= new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
		while (modifierTokenizer.hasMoreTokens()) {
			int modifier= Action.findModifier(modifierTokenizer.nextToken());
			if (modifier == 0 || (stateMask & modifier) == modifier)
				return -1;
			stateMask= stateMask | modifier;
		}
		return stateMask;
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
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, JavaHoverMessages.getString("JavaTextHover.createTextHover"), null)); //$NON-NLS-1$
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

	/**
	 * Returns the hover's description.
	 * 
	 * @return the hover's description or <code>null</code> if not provided
	 */
	public String getDescription() {
		return fElement.getAttribute(DESCRIPTION_ATTRIBUTE);
	}


	public boolean canActivatePlugIn() {
		return Boolean.valueOf(fElement.getAttribute(ACTIVATE_PLUG_IN_ATTRIBUTE)).booleanValue();
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

	private static JavaEditorTextHoverDescriptor[] createDescriptors(IConfigurationElement[] elements) {
		List result= new ArrayList(elements.length);
		for (int i= 0; i < elements.length; i++) {
			IConfigurationElement element= elements[i];
			if (HOVER_TAG.equals(element.getName())) {
				JavaEditorTextHoverDescriptor desc= new JavaEditorTextHoverDescriptor(element);
				result.add(desc);
			}
		}
		Collections.sort(result);
		return (JavaEditorTextHoverDescriptor[])result.toArray(new JavaEditorTextHoverDescriptor[result.size()]);
	}

	private static void initializeFromPreferences(JavaEditorTextHoverDescriptor[] hovers) {
		String compiledTextHoverModifiers= JavaPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS);
		
		StringTokenizer tokenizer= new StringTokenizer(compiledTextHoverModifiers, VALUE_SEPARATOR);
		HashMap idToModifier= new HashMap(tokenizer.countTokens() / 2);

		while (tokenizer.hasMoreTokens()) {
			String id= tokenizer.nextToken();
			if (tokenizer.hasMoreTokens())
				idToModifier.put(id, tokenizer.nextToken());
		}

		for (int i= 0; i < hovers.length; i++) {
			String modifierString= (String)idToModifier.get(hovers[i].getId());
			boolean enabled= true;
			if (modifierString == null)
				modifierString= DISABLED_TAG;
			
			if (modifierString.startsWith(DISABLED_TAG)) {
				enabled= false;
				modifierString= modifierString.substring(1);
			}

			if (modifierString.equals(NO_MODIFIER))
				modifierString= ""; //$NON-NLS-1$

			hovers[i].fModifierString= modifierString;
			hovers[i].fIsEnabled= enabled;
			hovers[i].fStateMask= computeStateMask(modifierString);
		}
	}

	/**
	 * Returns the configured modifier getStateMask for this hover.
	 * 
	 * @return the hover modifier stateMask or -1 if no hover is configured
	 */
	public int getStateMask() {
		return fStateMask;
	}

	/**
	 * Returns the modifier String as set in the preference store.
	 * 
	 * @return the modifier string
	 */
	public String getModifierString() {
		return fModifierString;
	}

	/**
	 * Returns whether this hover is enabled or not.
	 * 
	 * @return <code>true</code> if enabled
	 */
	public boolean isEnabled() {
		return fIsEnabled;
	}
}