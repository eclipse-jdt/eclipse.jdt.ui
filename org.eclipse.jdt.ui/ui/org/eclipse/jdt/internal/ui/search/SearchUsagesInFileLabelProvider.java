package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the SearchUsagesInFile results.
 * @see SearchUsagesInFileAction
 */
class SearchUsagesInFileLabelProvider extends JavaSearchResultLabelProvider {

	public String getText(Object o) {
		IMarker marker= getMarker(o);
		try {
			String text= (String)marker.getAttribute(IMarker.MESSAGE);
			if (text != null)
				return text;
		} catch (CoreException ex) {
			return "";  //$NON-NLS-1$
		}
		return super.getText(o);
	}
	
	public Image getImage(Object element) {
		IMarker marker= getMarker(element);
		if (isVariableAccess(marker)) {
			if (isWriteAccess(marker))
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_WRITEACCESS);
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_READACCESS);
		}
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
	}
	
	private boolean isWriteAccess(IMarker marker) {
		Boolean write= null;
		boolean writeValue= false;
		try {
			write= (Boolean)marker.getAttribute(SearchUsagesInFileAction.IS_WRITEACCESS);
			writeValue= write != null && write.booleanValue();
		} catch (CoreException e) {
		}
		return writeValue;
	}
	
	private boolean isVariableAccess(IMarker marker) {
		Boolean variable= null;
		boolean variableValue= false;
		try {
			variable= (Boolean)marker.getAttribute(SearchUsagesInFileAction.IS_VARIABLE);
			variableValue= variable != null && variable.booleanValue();
		} catch (CoreException e) {
		}
		return variableValue;
	}
}