package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;

import org.eclipse.jface.util.Assert;
import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

/**
 * The <code>JavaEditorErrorTickUpdater</code> will register as a AnnotationModelListener
 * on the annotation model of a Java Editor and update the title images when the annotation
 * model changed.
 */
public class JavaEditorErrorTickUpdater implements IAnnotationModelListener {

	private JavaEditor fJavaEditor;
	private IAnnotationModel fAnnotationModel;
	private JavaElementLabelProvider fLabelProvider;

	public JavaEditorErrorTickUpdater(JavaEditor editor) {
		fJavaEditor= editor;
		Assert.isNotNull(editor);
	}

	/**
	 * Defines the annotation model to listen to. To be called when the
	 * annotation model changes.
	 * @param model The new annotation model or <code>null</code>
	 * to uninstall.
	 */
	public void setAnnotationModel(IAnnotationModel model) {
		if (fAnnotationModel != null) {
			fAnnotationModel.removeAnnotationModelListener(this);
		}
				
		if (model != null) {
			fAnnotationModel=model;
			fAnnotationModel.addAnnotationModelListener(this);
			
			if (fLabelProvider == null) {
				fLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS);
			}
			fLabelProvider.setErrorTickManager(new AnnotationErrorTickProvider(fAnnotationModel));
			modelChanged(fAnnotationModel);
		} else {
			fAnnotationModel= null;
			if (fLabelProvider != null) {
				fLabelProvider.dispose();
				fLabelProvider= null;
			}
		}	
	}
			
	/**
	 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
	 */
	public void modelChanged(IAnnotationModel model) {
		Shell shell= fJavaEditor.getEditorSite().getShell();
		if (shell != null && !shell.isDisposed()) {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					doUpdateErrorTicks();
				}
			});
		}
	}
	
	private void doUpdateErrorTicks() {
		IEditorInput input= fJavaEditor.getEditorInput();
		if (fLabelProvider != null && input != null) { // running async, tests needed
			IJavaElement jelement= (IJavaElement) input.getAdapter(IJavaElement.class);
			if (jelement != null) {
				fJavaEditor.updatedTitleImage(fLabelProvider.getImage(jelement));
			}
		}
	}	

}


