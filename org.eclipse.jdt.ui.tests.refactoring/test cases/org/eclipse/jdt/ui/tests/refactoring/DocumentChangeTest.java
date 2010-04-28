/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.lang.reflect.InvocationTargetException;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.text.edits.InsertEdit;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Tests that {@link DocumentChange}s can be executed by a refactoring in a non-UI thread even if an editor is open.
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=296794 .
 *
 * @since 3.6
 */
public class DocumentChangeTest extends RefactoringTest {
	
	public static Test suite() {
		return setUpTest(new TestSuite(DocumentChangeTest.class));
	}

	public static Test suiteWithoutRefactoringTestSetup() {
		return new TestSetup(new TestSuite(DocumentChangeTest.class)) {
			protected void setUp() throws Exception {
				PlatformUI.getWorkbench().showPerspective(JavaUI.ID_PERSPECTIVE, JavaPlugin.getActiveWorkbenchWindow());
			}
			
			protected void tearDown() throws Exception {
				IWorkbenchPage activePage= JavaPlugin.getActivePage();
				if (activePage != null) {
					activePage.closeAllPerspectives(true, true);
				}
			}
		};
	}
	
	public static Test setUpTest(Test test) {
		return new RefactoringTestSetup(test) {
			protected void setUp() throws Exception {
				super.setUp(); // closes the perspective, need to reopen
				PlatformUI.getWorkbench().showPerspective(JavaUI.ID_PERSPECTIVE, JavaPlugin.getActiveWorkbenchWindow());
			}
		};
	}

	public DocumentChangeTest(String name) {
		super(name);
	}
	
	public void testDocumentChange() throws Exception {
		IProject project= RefactoringTestSetup.getProject().getProject();
		IFile file= project.getFile("file.txt");
		final String prolog= "This is a ";
		final String insertion= "modified ";
		final String epilog= "text";
		file.create(getStream(prolog + epilog), IResource.NONE, null);
		
		IEditorPart editor= IDE.openEditor(JavaPlugin.getActivePage(), file);
		ITextFileBuffer textFileBuffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
		final IDocument document= textFileBuffer.getDocument();
		
		final Refactoring ref= new Refactoring() {
			public String getName() {
				return getClass().getName();
			}
			
			public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
				return new RefactoringStatus();
			}
			
			public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
				return new RefactoringStatus();
			}
			
			public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
				DocumentChange change= new DocumentChange("DocumentChangeTest change", document);
				change.setEdit(new InsertEdit(prolog.length(), insertion));
				
				// need to provide a non-null affectedObjects from the undo change, otherwise the NonLocalUndoUserApprover shows a dialog.
				CompositeChange compositeChange= new CompositeChange("DocumentChangeTest composite change") {
					protected Change createUndoChange(Change[] childUndos) {
						return new CompositeChange(getName(), childUndos) {
							public Object[] getAffectedObjects() {
								return new Object[0];
							}
						};
					}
				};
				compositeChange.add(change);
				return compositeChange;
			}
		};
		
		final MultiStatus statusCollector= new MultiStatus(JavaTestPlugin.getPluginId(), 0, "", null);
		
		ILogListener logListener= new ILogListener() {
			public void logging(IStatus status, String plugin) {
				statusCollector.add(status);
			}
		};
		Platform.addLogListener(logListener);
		try {
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						performRefactoring(ref);
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			JavaPlugin.getActiveWorkbenchWindow().run(true, true, runnable);
			
			editor.doSave(new NullProgressMonitor());
			
			String contents= getContents(file);
			assertEquals(prolog + insertion + epilog, contents);
			
			// undo:
			
			runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			JavaPlugin.getActiveWorkbenchWindow().run(true, true, runnable);
			
			editor.doSave(new NullProgressMonitor());
			
			contents= getContents(file);
			assertEquals(prolog + epilog, contents);
			
			
		} finally {
			Platform.removeLogListener(logListener);
		}
		if (statusCollector.getChildren().length != 0) {
			throw new CoreException(statusCollector);
		}
		
	}
}
