package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

/**
 * The <code>JavaOutlineErrorTickUpdater</code> will register as a AnnotationModelListener on the annotation model
 * and update all images in the outliner tree when the annotation model changed.
 */
public class JavaOutlineErrorTickUpdater implements IAnnotationModelListener {

	private TreeViewer fViewer;
	private ILabelProvider fLabelProvider;
	private IAnnotationModel fAnnotationModel;

	/**
	 * @param viewer The viewer of the outliner.
	 * The label provider has to be a <code>JavaElementLabelProvider</code>,
	 * otherwise no error ticks wil be updated.
	 */
	public JavaOutlineErrorTickUpdater(TreeViewer viewer) {
		fViewer= viewer;
		IBaseLabelProvider lprovider= viewer.getLabelProvider();
		if (lprovider instanceof ILabelProvider) {
			fLabelProvider= (ILabelProvider) lprovider;
		}
	}

	/**
	 * Defines the annotation model to listen to. To be called when the
	 * annotation model changes.
	 * @param model The new annotation model or <code>null</code>
	 * to uninstall.
	 */
	public void setAnnotationModel(IAnnotationModel model) {
		if (fLabelProvider == null) {
			return;
		}
		
		if (fAnnotationModel != null) {
			fAnnotationModel.removeAnnotationModelListener(this);
		}
				
		if (model != null) {
			fAnnotationModel= model;
			fAnnotationModel.addAnnotationModelListener(this);
			modelChanged(model);
		} else {
			fAnnotationModel= null;
		}	
	}	
	
		
	/**
	 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
	 */
	public void modelChanged(IAnnotationModel model) {
		Control control= fViewer.getControl();
		if (control != null && !control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					// until we have deltas, update all error ticks
					doUpdateErrorTicks();
				}
			});
		}		
	}
	
	private void doUpdateErrorTicks() {
		// running async, have to check all
		if (fLabelProvider == null || fAnnotationModel == null) {
			return;
		}
		
		// do not look at class files
		if (!(fViewer.getInput() instanceof ICompilationUnit)) {
			return;
		}
		
		Tree tree= fViewer.getTree();
		if (!tree.isDisposed()) { // defensive code
			TreeItem[] items= fViewer.getTree().getItems();
			for (int i= 0; i < items.length; i++) {
				updateItem(items[i]);
			}
		}
	}
	
	private void updateItem(TreeItem item) {
		Object data= item.getData();
		if (data instanceof IJavaElement && ((IJavaElement)data).exists()) {
			Image old= item.getImage();
			Image image= fLabelProvider.getImage(data);
			if (image != null && image != old) {
				item.setImage(image);
			}
			TreeItem[] children= item.getItems();
			for (int i= 0; i < children.length; i++) {
				updateItem(children[i]);
			}
		}
	}	
	

}

