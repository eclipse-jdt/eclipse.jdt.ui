package org.eclipse.jdt.internal.junit.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * A dialog to select a test method.
 */
public class TestMethodSelectionDialog extends ElementListSelectionDialog {

	private IRunnableContext fRunnableContext;
	private IJavaElement fElement;

	public static class TestReferenceCollector implements IJavaSearchResultCollector {
		IProgressMonitor fMonitor;
		Set fResult= new HashSet(200);
		
		public TestReferenceCollector(IProgressMonitor pm) {
			fMonitor= pm;
		}
		
		public void aboutToStart() {
		}
	
		public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws CoreException {
			if (enclosingElement.getElementName().startsWith("test"))
				fResult.add(enclosingElement);
		}
	
		public void done() {
		}
	
		public IProgressMonitor getProgressMonitor() {
			return fMonitor;
		}
		
		public Object[] getResult() {
			return fResult.toArray();
		}
	}

	public TestMethodSelectionDialog(Shell shell, IRunnableContext context, IJavaElement element) {
		super(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_POST_QUALIFIED));
		fRunnableContext= context;
		fElement= element;
	}
	
	/*
	 * @see Windows#configureShell
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJUnitHelpContextIds.TEST_SELECTION_DIALOG);
	}

	/*
	 * @see Window#open()
	 */
	public int open() {
		Object[] elements;
		IType testType= findTestType();
		
		if (testType == null) {
			MessageDialog.openError(getShell(), "Select Test", "Cannot find '"+JUnitPlugin.TEST_INTERFACE_NAME+"' - make sure that JUnit is on the project's classpath.");
			return CANCEL;
		}
		
		try {
			elements= searchTestMethods(fElement, testType, fRunnableContext);
		} catch (InterruptedException e) {
			return CANCEL;
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getShell(), "Select Test", e.getTargetException().getMessage());
			return CANCEL;
		}
		
		if (elements.length == 0) {
			MessageDialog.openInformation(getShell(), "Go to Test", "No Tests Found that reference '"+fElement.getElementName()+"'.");
			return CANCEL;
		}
		setElements(elements);
		return super.open();
	}
	
	private IType findTestType() {
		String qualifiedName= JUnitPlugin.TEST_INTERFACE_NAME;
		IJavaProject[] projects;
		try {
			projects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
			for (int i= 0; i < projects.length; i++) {
				IJavaProject project= projects[i];
				IType type= project.findType(qualifiedName);
				if (type != null) {
					return type;
				}
			}
		} catch (JavaModelException e) {
		}
		return null;
	}
	
	public Object[] searchTestMethods(final IJavaElement element, final IType testType, IRunnableContext context) throws InvocationTargetException, InterruptedException  {
		final TestReferenceCollector[] col= new TestReferenceCollector[1];
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					col[0]= doSearchTestMethods(element, testType, pm);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		context.run(true, true, runnable);
		return col[0].getResult();
	}

	private TestReferenceCollector doSearchTestMethods(IJavaElement element, IType testType, IProgressMonitor pm) throws JavaModelException{
		IJavaSearchScope scope= SearchEngine.createHierarchyScope(testType);
		TestReferenceCollector collector= new TestReferenceCollector(pm);
		new SearchEngine().search(ResourcesPlugin.getWorkspace(), element, IJavaSearchConstants.REFERENCES, scope, collector);
		return collector;
	}
}