package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginPrerequisite;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.preferences.JavaEditorTextHoverDescriptor;


/**
 * Caution: this implementation is a layer breaker and contains some "shortcuts"
 */
public class JavaTextHover extends AbstractJavaEditorTextHover {
		
	private static String ID= "org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover"; //$NON-NLS-1$
	private static String JAVA_PROBLEM_HOVER_ID= "org.eclipse.jdt.internal.ui.text.java.hover.JavaProblemHover"; //$NON-NLS-1$


	private static class ConfigurationElementComparator implements Comparator {
		
		/*
		 * @see Comparator#compare(Object, Object)
		 */
		public int compare(Object object0, Object object1) {

			IConfigurationElement element0= (IConfigurationElement)object0;
			IConfigurationElement element1= (IConfigurationElement)object1;	

			String id0=	element0.getAttribute(JavaEditorTextHoverDescriptor.ID_ATTRIBUTE);
			String id1= element1.getAttribute(JavaEditorTextHoverDescriptor.ID_ATTRIBUTE);
			
			String problemHoverId= JAVA_PROBLEM_HOVER_ID;
			
			if (id0 != null && id0.equals(id1))
				return 0;
			
			if (id0 != null && id0.equals(problemHoverId))
				return -1;

			if (id1 != null && id1.equals(problemHoverId))
				return +1;

			// now compare non-problem hovers

			if (dependsOn(element0, element1))
				return -1;
				
			if (dependsOn(element1, element0))
				return +1;
			
			return 0;
		}

		private static boolean dependsOn(IConfigurationElement element0, IConfigurationElement element1) {
			IPluginDescriptor descriptor0= element0.getDeclaringExtension().getDeclaringPluginDescriptor();
			IPluginDescriptor descriptor1= element1.getDeclaringExtension().getDeclaringPluginDescriptor();
			
			return dependsOn(descriptor0, descriptor1);
		}
		
		private static boolean dependsOn(IPluginDescriptor descriptor0, IPluginDescriptor descriptor1) {

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
	}
	
	protected String fCurrentPerspectiveId;
	protected List fTextHoverSpecifications;
	protected List fInstantiatedTextHovers;


	public JavaTextHover() {
		installTextHovers();
	}

	public JavaTextHover(IEditorPart editor) {
		this();
		setEditor(editor);
	}
	
	/**
	 * Installs all text hovers.
	 */
	private void installTextHovers() {
		
		// initialize lists - indicates that the initialization happened
		fTextHoverSpecifications= new ArrayList(2);
		fInstantiatedTextHovers= new ArrayList(2);

		// populate list
		IExtensionPoint extensionPoint= Platform.getPluginRegistry().getExtensionPoint(JavaPlugin.getPluginId(), "javaEditorTextHovers"); //$NON-NLS-1$
		if (extensionPoint != null) {
			IConfigurationElement[] elements= extensionPoint.getConfigurationElements();			
			for (int i= 0; i < elements.length; i++)
				// ensure that we don't add ourselves to the list
				if (!ID.equals(elements[i].getAttribute(JavaEditorTextHoverDescriptor.ID_ATTRIBUTE)))
					fTextHoverSpecifications.add(elements[i]);
		}
		
		Collections.sort(fTextHoverSpecifications, new ConfigurationElementComparator());
	}	


	private void checkTextHovers() {
		if (fTextHoverSpecifications.size() == 0)
			return;

		for (Iterator iterator= fTextHoverSpecifications.iterator(); iterator.hasNext(); ) {
			IConfigurationElement spec= (IConfigurationElement) iterator.next();

			IJavaEditorTextHover hover= createTextHover(spec);
			if (hover != null) {
				hover.setEditor(getEditor());
				addTextHover(hover);					
			}
		}

		fTextHoverSpecifications.clear();
	}

	protected void addTextHover(ITextHover hover) {
		if (!fInstantiatedTextHovers.contains(hover))
			fInstantiatedTextHovers.add(hover);
	}

	/**
	 * Creates a text hover as specified in the given configuration
	 * element.
	 */
	private IJavaEditorTextHover createTextHover(IConfigurationElement element) {
		return new JavaEditorTextHoverDescriptor(element).createTextHover();
	}
	
	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {

		checkTextHovers();

		if (fInstantiatedTextHovers == null)
			return null;

		for (Iterator iterator= fInstantiatedTextHovers.iterator(); iterator.hasNext(); ) {
			ITextHover hover= (ITextHover) iterator.next();

			String s= hover.getHoverInfo(textViewer, hoverRegion);
			if (s != null && s.trim().length() > 0)
				return s;
		}

		return null;
	}
}