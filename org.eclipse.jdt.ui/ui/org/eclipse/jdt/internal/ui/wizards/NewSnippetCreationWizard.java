/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.resource.ImageDescriptor;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.wizard.Wizard;import org.eclipse.ui.INewWizard;import org.eclipse.ui.IWorkbench;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Creates a new snippet page
 */
 
public class NewSnippetCreationWizard extends Wizard implements INewWizard {

	private NewSnippetFileCreationPage fPage;
	private IWorkbench fWorkbench;
	private IStructuredSelection fSelection;
	private ImageDescriptor fImage;
	
	public NewSnippetCreationWizard() {
		setNeedsProgressMonitor(true);
	}

	/**
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		if (fSelection == null) {
			IJavaElement elem= EditorUtility.getActiveEditorJavaInput();
			if (elem != null) {
				fSelection= new StructuredSelection(elem);
			} else {
				fSelection= StructuredSelection.EMPTY;
			}
		}
		fPage= new NewSnippetFileCreationPage(fWorkbench, fSelection);
		addPage(fPage);
	}

	/**
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		return fPage.finish();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fWorkbench= workbench;
		fSelection= selection;
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWCLASS);
	}
}
