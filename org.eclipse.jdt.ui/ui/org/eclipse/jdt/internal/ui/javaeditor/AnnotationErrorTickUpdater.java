package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IWorkingCopy;

public class AnnotationErrorTickUpdater implements IAnnotationModelListener {

	private TreeViewer fViewer;
	private IAnnotationModel fAnnotationModel;

	public AnnotationErrorTickUpdater(TreeViewer viewer) {
		fViewer= viewer;
	}
	
	public void install(ITextEditor editor) {
		fAnnotationModel= editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		fAnnotationModel.addAnnotationModelListener(this);
		
	}
	
	public void uninstall() {
		if (fAnnotationModel != null) {
			fAnnotationModel.removeAnnotationModelListener(this);
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
		// do not look at class files
		if (!(fViewer.getInput() instanceof IWorkingCopy)) {
			return;
		}
		
		Tree tree= fViewer.getTree();
		ILabelProvider lprovider= (ILabelProvider)fViewer.getLabelProvider();
		
		if (!tree.isDisposed()) { // defensive code
			TreeItem[] items= fViewer.getTree().getItems();
			for (int i= 0; i < items.length; i++) {
				updateItem(lprovider, items[i]);
			}
		}
	}
	
	private void updateItem(ILabelProvider lprovider, TreeItem item) {
		Object data= item.getData();
		Image old= item.getImage();
		Image image= lprovider.getImage(data);
		if (image != null && image != old) {
			item.setImage(image);
		}
		TreeItem[] children= item.getItems();
		for (int i= 0; i < children.length; i++) {
			updateItem(lprovider, children[i]);
		}
	}	
	

}

