/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.IRefactoringCoreStatusCodes;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring.CleanUpChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.IPostSaveListener;

public class CleanUpPostSaveListener implements IPostSaveListener {
	
	private static class CleanUpSaveUndo extends TextFileChange {

		private final IFile fFile;
		private final UndoEdit[] fUndos;
		private final long fDocumentStamp;
		private final long fFileStamp;

		public CleanUpSaveUndo(String name, IFile file, UndoEdit[] undos, long documentStamp, long fileStamp) {
			super(name, file);
			Assert.isNotNull(undos);
			
			fDocumentStamp= documentStamp;
			fFileStamp= fileStamp;
			fFile= file;
			fUndos= undos;
		}

		public final boolean needsSaving() {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		public Change perform(IProgressMonitor pm) throws CoreException {
			if (isValid(pm).hasFatalError())
				return new NullChange();
			
			if (pm == null)
				pm= new NullProgressMonitor();
			
			ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
			pm.beginTask("", 2); //$NON-NLS-1$
			ITextFileBuffer buffer= null;
			try {
				manager.connect(fFile.getFullPath(), LocationKind.IFILE, new SubProgressMonitor(pm, 1));
				buffer= manager.getTextFileBuffer(fFile.getFullPath(), LocationKind.IFILE);
				IDocument document= buffer.getDocument();
				
				long oldFileValue= fFile.getModificationStamp();
				long oldDocValue;
				if (document instanceof IDocumentExtension4) {
					oldDocValue= ((IDocumentExtension4)document).getModificationStamp();
				} else {
					oldDocValue= oldFileValue;
				}

				// perform the changes
				LinkedList list= new LinkedList();
				for (int index= 0; index < fUndos.length; index++) {
					UndoEdit edit= fUndos[index];
					UndoEdit redo= edit.apply(document, TextEdit.CREATE_UNDO);
					list.addFirst(redo);
				}

				boolean stampSetted= false;
				
				if (document instanceof IDocumentExtension4 && fDocumentStamp != IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP) {
					try {
						((IDocumentExtension4)document).replace(0, 0, "", fDocumentStamp); //$NON-NLS-1$
						stampSetted= true;
					} catch (BadLocationException e) {
						String message= e.getMessage();
                        if (message == null)
                        	message= "BadLocationException"; //$NON-NLS-1$
						throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IRefactoringCoreStatusCodes.BAD_LOCATION, message, e));
					}
				}
				
				buffer.commit(pm, false);
				if (!stampSetted) {
					fFile.revertModificationStamp(fFileStamp);
				}
				
				return new CleanUpSaveUndo(getName(), fFile, ((UndoEdit[]) list.toArray(new UndoEdit[list.size()])), oldDocValue, oldFileValue);
			} catch (BadLocationException e) {
				String message= e.getMessage();
                if (message == null)
                	message= "BadLocationException"; //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IRefactoringCoreStatusCodes.BAD_LOCATION, message, e));
			} finally {
				if (buffer != null)
					manager.disconnect(fFile.getFullPath(), LocationKind.IFILE, new SubProgressMonitor(pm, 1));
			}
		}
	}

	public static final String POSTSAVELISTENER_ID= "org.eclipse.jdt.ui.postsavelistener.cleanup"; //$NON-NLS-1$
	private static final String WARNING_VALUE= "warning"; //$NON-NLS-1$
	private static final String ERROR_VALUE= "error"; //$NON-NLS-1$

	/**
	 * {@inheritDoc}
	 */
	public void saved(ICompilationUnit unit, boolean saveAs, IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		
		try {
			if (!ActionUtil.isOnBuildPath(unit))
				return;
			
			IProject project= unit.getJavaProject().getProject();
			Map settings= CleanUpPreferenceUtil.loadSaveParticipantOptions(new ProjectScope(project));
			if (settings == null) {
				IEclipsePreferences contextNode= new InstanceScope().getNode(JavaUI.ID_PLUGIN);
				String id= contextNode.get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, null);
				if (id == null) {
					id= new DefaultScope().getNode(JavaUI.ID_PLUGIN).get(CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, CleanUpConstants.DEFAULT_SAVE_PARTICIPANT_PROFILE);
				}
				throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, Messages.format(FixMessages.CleanUpPostSaveListener_unknown_profile_error_message, id)));
			}
			
			ICleanUp[] cleanUps;
			if (CleanUpConstants.TRUE.equals(settings.get(CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS))) {
				cleanUps= CleanUpRefactoring.createCleanUps(settings);
			} else {
				HashMap filteredSettins= new HashMap();
				filteredSettins.put(CleanUpConstants.FORMAT_SOURCE_CODE, settings.get(CleanUpConstants.FORMAT_SOURCE_CODE));
				filteredSettins.put(CleanUpConstants.ORGANIZE_IMPORTS, settings.get(CleanUpConstants.ORGANIZE_IMPORTS));
				cleanUps= CleanUpRefactoring.createCleanUps(filteredSettins);
			}
			
			if (saveAs) {
				do {					
					List undoneCleanUps= new ArrayList();
					CleanUpChange change= calculateChange(unit, cleanUps, undoneCleanUps, monitor);
					if (change != null) {
						Document document= new Document(unit.getBuffer().getContents());
						try {
							change.getEdit().apply(document);
						} catch (MalformedTreeException e) {
							throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, 0, e.getLocalizedMessage(), e));
						} catch (BadLocationException e) {
							String message= e.getLocalizedMessage();
			                if (message == null)
			                	message= "BadLocationException"; //$NON-NLS-1$
							throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IRefactoringCoreStatusCodes.BAD_LOCATION, message, e));
						}
						unit.getBuffer().setContents(document.get());
					}
					cleanUps= (ICleanUp[])undoneCleanUps.toArray(new ICleanUp[undoneCleanUps.size()]);
				} while (cleanUps.length > 0);
			} else {
				long oldFileValue= unit.getResource().getModificationStamp();
				long oldDocValue= getDocumentStamp((IFile)unit.getResource(), new SubProgressMonitor(monitor, 2));
				
				CompositeChange result= new CompositeChange(FixMessages.CleanUpPostSaveListener_SaveAction_ChangeName);
				LinkedList undoEdits= new LinkedList();
				
				IUndoManager manager= RefactoringCore.getUndoManager();
				
				try {
	    			manager.aboutToPerformChange(result);
	    			
	    			do {
	    				List undoneCleanUps= new ArrayList();
	    				CleanUpChange change= calculateChange(unit, cleanUps, undoneCleanUps, monitor);
	    				
	    				if (change != null) {
	    					result.add(change);
	    					
	    					change.setSaveMode(TextFileChange.LEAVE_DIRTY);
	    					change.initializeValidationData(new NullProgressMonitor());
	    					
	    					PerformChangeOperation performChangeOperation= RefactoringUI.createUIAwareChangeOperation(change);
	    					performChangeOperation.setSchedulingRule(unit.getSchedulingRule());
	    					
	    					performChangeOperation.run(new SubProgressMonitor(monitor, 5));
	    					
	    					performChangeOperation.getUndoChange();
	    					undoEdits.addFirst(change.getUndoEdit());
	    				}
	    				
	    				cleanUps= (ICleanUp[])undoneCleanUps.toArray(new ICleanUp[undoneCleanUps.size()]);
	    			} while (cleanUps.length > 0);
				} finally {
					manager.changePerformed(result, true);
				}
				
				if (undoEdits.size() > 0) {
	    			UndoEdit[] undoEditArray= (UndoEdit[])undoEdits.toArray(new UndoEdit[undoEdits.size()]);
	    			CleanUpSaveUndo undo= new CleanUpSaveUndo(result.getName(), (IFile)unit.getResource(), undoEditArray, oldDocValue, oldFileValue);
	    			undo.initializeValidationData(new NullProgressMonitor());
	    			manager.addUndo(result.getName(), undo);
				}
			}
		} finally {
			monitor.done();
		}
	}

	private CleanUpChange calculateChange(ICompilationUnit unit, ICleanUp[] cleanUps, List undoneCleanUps, IProgressMonitor monitor) throws CoreException {
		RefactoringStatus preCondition= new RefactoringStatus();
		for (int i= 0; i < cleanUps.length; i++) {
			RefactoringStatus conditions= cleanUps[i].checkPreConditions(unit.getJavaProject(), new ICompilationUnit[] {unit}, new SubProgressMonitor(monitor, 5));
			preCondition.merge(conditions);
		}
		if (showStatus(preCondition) != Window.OK) {
			undoneCleanUps.clear();
			return null;
		}
		
		Map options= new HashMap();
		for (int i= 0; i < cleanUps.length; i++) {
			Map map= cleanUps[i].getRequiredOptions();
			if (map != null) {
				options.putAll(map);
			}
		}
			
		CompilationUnit ast= null;
		if (requiresAST(cleanUps, unit)) {
			ast= createAst(unit, options, new SubProgressMonitor(monitor, 10));
		}
		
		CleanUpChange change= CleanUpRefactoring.calculateChange(ast, unit, cleanUps, undoneCleanUps);
		
		RefactoringStatus postCondition= new RefactoringStatus();
		for (int i= 0; i < cleanUps.length; i++) {
			RefactoringStatus conditions= cleanUps[i].checkPostConditions(new SubProgressMonitor(monitor, 1));
			postCondition.merge(conditions);
		}
		if (showStatus(postCondition) != Window.OK) {
			undoneCleanUps.clear();
			return null;
		}
		
		return change;
	}

	private int showStatus(RefactoringStatus status) {
		if (!status.hasError())
			return Window.OK;

		Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		Dialog dialog= RefactoringUI.createRefactoringStatusDialog(status, shell, "", false); //$NON-NLS-1$
		return dialog.open(); 
    }

	private long getDocumentStamp(IFile file, IProgressMonitor monitor) throws CoreException {
	    final ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
	    final IPath path= file.getFullPath();

	    monitor.beginTask("", 2); //$NON-NLS-1$
	    
	    ITextFileBuffer buffer= null;
	    try {
	    	manager.connect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
		    buffer= manager.getTextFileBuffer(path, LocationKind.IFILE);
    	    IDocument document= buffer.getDocument();
    	    
    	    if (document instanceof IDocumentExtension4) {
    			return ((IDocumentExtension4)document).getModificationStamp();
    		} else {
    			return file.getModificationStamp();
    		}
	    } finally {
	    	if (buffer != null)
	    		manager.disconnect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
	    	monitor.done();
	    }
    }

	private boolean requiresAST(ICleanUp[] cleanUps, ICompilationUnit unit) throws CoreException {
		for (int i= 0; i < cleanUps.length; i++) {
	        if (cleanUps[i].requireAST(unit))
	        	return true;
        }
		
	    return false;
    }

	private CompilationUnit createAst(ICompilationUnit unit, Map cleanUpOptions, IProgressMonitor monitor) {
		IJavaProject project= unit.getJavaProject();
		if (compatibleOptions(project, cleanUpOptions)) {
			CompilationUnit ast= ASTProvider.getASTProvider().getAST(unit, ASTProvider.WAIT_NO, monitor);
			if (ast != null)
				return ast;
		}
		
		ASTParser parser= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setProject(project);
		parser.setSource(unit);
		Map compilerOptions= RefactoringASTParser.getCompilerOptions(unit.getJavaProject());
		compilerOptions.putAll(cleanUpOptions);
		parser.setCompilerOptions(compilerOptions);
		
		return (CompilationUnit)parser.createAST(monitor);
	}
	
	private boolean compatibleOptions(IJavaProject project, Map cleanUpOptions) {
		if (cleanUpOptions.size() == 0)
			return true;
		
		Map projectOptions= project.getOptions(true);
		
		for (Iterator iterator= cleanUpOptions.keySet().iterator(); iterator.hasNext();) {
	        String key= (String)iterator.next();
	        String projectOption= (String)projectOptions.get(key);
			String cleanUpOption= (String)cleanUpOptions.get(key);
			if (!strongerEquals(projectOption, cleanUpOption))
				return false;
        }
		
	    return true;
    }

	private boolean strongerEquals(String projectOption, String cleanUpOption) {
		if (projectOption == null)
			return false;
		
		if (ERROR_VALUE.equals(cleanUpOption)) {
			return ERROR_VALUE.equals(projectOption);
		} else if (WARNING_VALUE.equals(cleanUpOption)) {
			return ERROR_VALUE.equals(projectOption) || WARNING_VALUE.equals(projectOption);
		}
		
	    return false;
    }

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return FixMessages.CleanUpPostSaveListener_name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getId() {
		return POSTSAVELISTENER_ID;
	}
	
}
