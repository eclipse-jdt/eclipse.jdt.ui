package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.viewsupport.ErrorTickImageProvider;

/**
 * The <code>JavaEditorErrorTickUpdater</code> will register as a AnnotationModelListener
 * on the annotation model of a Java Editor and update the title images when the annotation
 * model changed.
 */
public class JavaEditorErrorTickUpdater implements IAnnotationModelListener {

	private JavaEditor fJavaEditor;
	private IAnnotationModel fAnnotationModel;
	private ErrorTickImageProvider fImageProvider;

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
			if (fImageProvider == null) {
				fImageProvider= new ErrorTickImageProvider();
			}
			fAnnotationModel=model;
			fAnnotationModel.addAnnotationModelListener(this);
			modelChanged(fAnnotationModel);
		} else {
			if (fImageProvider != null) {
				fImageProvider.dispose();
			}
			fImageProvider= null;
			fAnnotationModel= null;
		}	
	}
			
	/**
	 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
	 */
	public void modelChanged(IAnnotationModel model) {
		Image titleImage= fJavaEditor.getTitleImage();
		if (titleImage == null) {
			return;
		}
		IEditorInput input= fJavaEditor.getEditorInput();
		if (input != null) { // might run async, tests needed
			IJavaElement jelement= (IJavaElement) input.getAdapter(IJavaElement.class);
			if (fImageProvider != null && jelement != null) {
				Image newImage= fImageProvider.getImageLabel(jelement, ErrorTickImageProvider.OVERLAY_ICONS | ErrorTickImageProvider.SMALL_ICONS);
				if (titleImage != newImage) {
					updatedTitleImage(newImage);
				}
			}
		}
	}
	
	private void updatedTitleImage(final Image newImage) {
		Shell shell= fJavaEditor.getEditorSite().getShell();
		if (shell != null && !shell.isDisposed()) {
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					fJavaEditor.updatedTitleImage(newImage);
				}
			});
		}
	}	
	
}


