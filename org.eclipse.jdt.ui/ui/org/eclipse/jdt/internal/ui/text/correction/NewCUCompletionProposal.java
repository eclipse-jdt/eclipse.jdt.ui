package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Creates a new CU with primary type
  */
public class NewCUCompletionProposal extends ChangeCorrectionProposal {
	
	private ICompilationUnit fCompilationUnit;
	private boolean fIsClass;
	
	/**
	 * Constructor for NewCUCompletionProposal.
	 */
	public NewCUCompletionProposal(String name, ICompilationUnit addedCU, boolean isClass, String[] superTypes, int severity) {
		super(name, new CreateCompilationUnitChange(addedCU, isClass, superTypes, JavaPreferencesSettings.getCodeGenerationSettings()), severity, null);
	
		fCompilationUnit= addedCU;
		fIsClass= isClass;
		if (fIsClass) {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS));
		} else {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE));
		}
	}

	/* (non-Javadoc)
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		super.apply(document);
		try {
			IEditorPart part= EditorUtility.openInEditor(fCompilationUnit, true);
			EditorUtility.revealInEditor(part, fCompilationUnit.findPrimaryType());
		} catch (PartInitException e) {
			JavaPlugin.log(e);
		} catch (JavaModelException e) {
			JavaPlugin.logIgnoringNotPresentException(e);
		}
	}
}
