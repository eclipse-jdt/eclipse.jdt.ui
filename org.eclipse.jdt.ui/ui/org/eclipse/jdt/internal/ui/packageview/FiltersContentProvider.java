package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import java.util.Vector;

/**
 * The FiltersContentProvider provides the elements for use by the list dialog
 * for selecting the patterns to apply.
 */ 
class FiltersContentProvider implements IStructuredContentProvider {

	private JavaElementPatternFilter fJavaFilter; 
	
	/**
	 * Create a FiltersContentProvider using the selections from the suppliec
	 * resource filter.
	 */
	public FiltersContentProvider(JavaElementPatternFilter filter) {
		super();
		fJavaFilter= filter;
	}
	/**
	 * Disposes of this content provider.  
	 * This is called by the viewer when it is disposed.
	 */
	public void dispose() {}
	/**
	 * Returns the filters currently defined for the workbench. 
	 */
	public static Vector getDefinedFilters() { 
		JavaPlugin plugin= JavaPlugin.getDefault();
	
		IExtensionPoint extension = plugin.getDescriptor().getExtensionPoint(JavaElementPatternFilter.FILTERS_TAG);
		IExtension[] extensions =  extension.getExtensions();
	
		Vector elements = new Vector();
		for(int i = 0; i < extensions.length; i++){
			IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
			for(int j = 0; j < configElements.length; j++){
				String pattern = configElements[j].getAttribute(JavaElementPatternFilter.PATTERN);
				if(pattern != null)
					elements.add(pattern);
			}		
		}
		return elements;
	}
	
	/**
	 * Returns the elements to display in the viewer 
	 * when its input is set to the given element. 
	 * These elements can be presented as rows in a table, items in a list, etc.
	 * The result is not modified by the viewer.
	 *
	 * @param inputElement the input element
	 * @return the array of elements to display in the viewer
	 */
	public Object[] getElements(Object inputElement) {
		return getDefinedFilters().toArray();
	}
	/**
	 * Return the initially selected values
	 * @return java.lang.String[]
	 */
	public String[] getInitialSelections() {
		return fJavaFilter.getPatterns();
	}
	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
