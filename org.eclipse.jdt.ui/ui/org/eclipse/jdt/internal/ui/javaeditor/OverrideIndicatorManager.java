/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AnnotationPreference;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;

/**
 * Manages the override and overwrite indicators for
 * the given Java element and annotation model.
 * 
 * @since 3.0
 */
class OverrideIndicatorManager implements IJavaReconcilingListener {
	
	/**
	 * Overwrite and override indicator annotation.
	 * 
	 * @since 3.0
	 */
	class OverrideIndicator extends Annotation {
		
		private boolean fIsOverwriteIndicator;
		private String fAstNodeKey;
		
		/**
		 * Creates a new override annotation.
		 * 
		 * @param isOverwriteIndicator <code>true</code> if this annotation is
		 *            an overwrite indicator, <code>false</code> otherwise
		 * @param text the text associated with this annotation
		 * @param key the method binding key
		 * @since 3.0
		 */
		OverrideIndicator(boolean isOverwriteIndicator, String text, String key) {
			super(ANNOTATION_TYPE, false, text);
			fIsOverwriteIndicator= isOverwriteIndicator;
			fAstNodeKey= key;
		}

		/**
		 * Tells whether this is an overwrite or an override indicator.
		 * 
		 * @return <code>true</code> if this is an overwrite indicator
		 */
		public boolean isOverwriteIndicator() {
			return fIsOverwriteIndicator;
		}

		/**
		 * Opens and reveals the defining method.
		 */
		public void open() {
			CompilationUnit ast= JavaPlugin.getDefault().getASTProvider().getAST(fJavaElement, true, null);
			ASTNode node= null;
			if (ast != null)
				node= ast.findDeclaringNode(fAstNodeKey);
			if (node instanceof MethodDeclaration) {
				try {
					IMethodBinding methodBinding= ((MethodDeclaration)node).resolveBinding();
					IType type= getType(methodBinding);
					
					if (type != null) {
					
						ITypeHierarchy th= SuperTypeHierarchyCache.getTypeHierarchy(type);
						IType[] superTypes= th.getAllSupertypes(type);
						
						IMethodBinding definingMethodBinding= Bindings.findMethodDefininition(methodBinding);
						IType definingType= findType(superTypes, definingMethodBinding.getDeclaringClass().getQualifiedName());
						
						if (definingType != null) {
							IMethod definingMethod= Bindings.findMethod(definingMethodBinding, definingType);
							if (definingMethod != null) {
								OpenActionUtil.open(definingMethod, true);
								return;
							}
						}
					}
				} catch (JavaModelException ex) {
					JavaPlugin.log(ex.getStatus());
				} catch (PartInitException ex) {
					JavaPlugin.log(ex.getStatus());
				}
			}
			String title= JavaEditorMessages.getString("OverrideIndicatorManager.open.error.title"); //$NON-NLS-1$
			String message= JavaEditorMessages.getString("OverrideIndicatorManager.open.error.message"); //$NON-NLS-1$
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message);
		}
		
		private IType getType(IMethodBinding methodBinding) throws JavaModelException {
			if (fJavaElement == null || methodBinding == null)
				return null;
			
			int type= fJavaElement.getElementType();
			
			IType[] types= null;
			if (type == IJavaElement.COMPILATION_UNIT)
				types= ((ICompilationUnit)fJavaElement).getAllTypes();
			else if (type == IJavaElement.CLASS_FILE)
				types= new IType[] { ((IClassFile)fJavaElement).getType() };
			
			return findType(types, methodBinding.getDeclaringClass().getQualifiedName());
		}
		
		private IType findType(IType[] types, String fullyQualifiedName) throws JavaModelException {
			if (types == null)
				return null;
			
			for (int i= 0; i < types.length; i++) {
				IType type= types[i];
				if (JavaModelUtil.getFullyQualifiedName(type).equals(fullyQualifiedName))
					return type;
			}
			return null;
		}
	}
	
	static final String ANNOTATION_TYPE= "org.eclipse.jdt.ui.overrideIndicator"; //$NON-NLS-1$

	private OverrideIndicatorJob fOverrideIndicatorJob;
	private IAnnotationModel fAnnotationModel;
	private AnnotationPreference fAnnotationPreference;
	private Annotation[] fOverrideAnnotations;
	private boolean fCancelled= false;
	private IJavaElement fJavaElement;

	
	public OverrideIndicatorManager(IAnnotationModel annotationModel, IJavaElement javaElement, CompilationUnit ast) {
		this(annotationModel);
		fJavaElement= javaElement;
		updateAnnotations(ast);
	}
	
	public OverrideIndicatorManager(IAnnotationModel annotationModel) {
		Assert.isNotNull(annotationModel);
		fAnnotationModel=annotationModel; 
		
		fAnnotationPreference= EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreferenceFragment(ANNOTATION_TYPE);
	}
	

	/**
	 * Creates and adds the override annotations.
	 * 
	 * XXX: Delete if not needed. 
	 */
	class OverrideIndicatorJob extends Job {
		
		private IDocument fDocument;
		private IProgressMonitor fProgressMonitor;
		private Position[] fPositions;

		public OverrideIndicatorJob(Position[] positions) {
			this(null, positions);
		}

		public OverrideIndicatorJob(IDocument document, Position[] positions) {
			super("Override Indicator"); //$NON-NLS-1$
			fDocument= document;
			fPositions= positions;
		}
		
		private boolean isCancelled() {
			return fCancelled || fProgressMonitor.isCanceled();
		}
		
		/*
		 * @see Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public IStatus run(IProgressMonitor progressMonitor) {
			
			fProgressMonitor= progressMonitor;
			
			if (isCancelled())
				return Status.CANCEL_STATUS;
			
			// Add annotations
			int length= fPositions.length;
			Map annotationMap= new HashMap(length);
			for (int i= 0; i < length; i++) {
				
				if (isCancelled())
					return Status.CANCEL_STATUS; 
				
				String message;
				Position position= fPositions[i];
				
				// Create & add annotation
				try {
					// FIXME
					message= "document.get(position.offset, position.length)";
					if (false)
						throw new BadLocationException();
				} catch (BadLocationException ex) {
					// Skip this match
					continue;
				}
				annotationMap.put(
						new Annotation(ANNOTATION_TYPE, false, message), //$NON-NLS-1$
						position);
			}
			
			if (isCancelled())
				return Status.CANCEL_STATUS;
			
			synchronized (fAnnotationModel) {
				if (fAnnotationModel instanceof IAnnotationModelExtension) {
					((IAnnotationModelExtension)fAnnotationModel).replaceAnnotations(fOverrideAnnotations, annotationMap);
				} else {
					removeAnnotations();
					Iterator iter= annotationMap.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry mapEntry= (Map.Entry)iter.next(); 
						fAnnotationModel.addAnnotation((Annotation)mapEntry.getKey(), (Position)mapEntry.getValue());
					}
				}
				fOverrideAnnotations= (Annotation[])annotationMap.keySet().toArray(new Annotation[annotationMap.keySet().size()]);
			}

			return Status.OK_STATUS;
		}
	}	

	/**
	 * Updates the override and implements annotations based
	 * on the given AST.
	 * 
	 * @param ast the compilation unit ast
	 * @since 3.0
	 */
	protected void updateAnnotations(CompilationUnit ast) {

		if (fOverrideIndicatorJob != null)
			fOverrideIndicatorJob.cancel();

		if (!showOverrideIndicators()) {
			removeAnnotations();
			return;
		}
		
		if (ast == null)
			return;
		
		final Map annotationMap= new HashMap(50);

		ast.accept(new ASTVisitor(false) {
			/*
			 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
			 */
			public boolean visit(MethodDeclaration node) {
				IMethodBinding binding= node.resolveBinding();
				if (binding != null) {
					IMethodBinding definingMethod= Bindings.findMethodDefininition(binding);
					if (definingMethod != null) {
						
						ITypeBinding definingType= definingMethod.getDeclaringClass();
						String qualifiedMethodName= definingType.getQualifiedName() + "." + binding.getName(); //$NON-NLS-1$
						
						boolean isOverwriteIndicator= definingType.isInterface();
						String text;
						if (isOverwriteIndicator)
							text= JavaEditorMessages.getFormattedString("OverrideIndicatorManager.implements", qualifiedMethodName); //$NON-NLS-1$
						else
							text= JavaEditorMessages.getFormattedString("OverrideIndicatorManager.overrides", qualifiedMethodName); //$NON-NLS-1$
						
						SimpleName name= node.getName();
						Position position= new Position(name.getStartPosition(), name.getLength());

						annotationMap.put(
								new OverrideIndicator(isOverwriteIndicator, text, binding.getKey()), //$NON-NLS-1$
								position);

					}
				}
				return false;
			}
		});
		
		if (fCancelled)
			return;
		
		synchronized (fAnnotationModel) {
			if (fAnnotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension)fAnnotationModel).replaceAnnotations(fOverrideAnnotations, annotationMap);
			} else {
				removeAnnotations();
				Iterator iter= annotationMap.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry mapEntry= (Map.Entry)iter.next(); 
					fAnnotationModel.addAnnotation((Annotation)mapEntry.getKey(), (Position)mapEntry.getValue());
				}
			}
			fOverrideAnnotations= (Annotation[])annotationMap.keySet().toArray(new Annotation[annotationMap.keySet().size()]);
		}
		
		// XXX: code we might want to use when processing the annotations in a separate job 
		
//		Position[] positions= (Position[])matches.toArray(new Position[matches.size()]);
//		
//		fOverrideIndicatorJob= new OverrideIndicatorJob(positions);
//		//fOccurrencesFinderJob.setPriority(Job.DECORATE);
//		//fOccurrencesFinderJob.setSystem(true);
//		//fOccurrencesFinderJob.schedule();
//		fOverrideIndicatorJob.run(new NullProgressMonitor());
	}

	/**
	 * Removes all override indicators from this manager's annotation model. 
	 */
	void removeAnnotations() {
		if (fOverrideAnnotations == null)
			return;

		synchronized (fAnnotationModel) {
			if (fAnnotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension)fAnnotationModel).replaceAnnotations(fOverrideAnnotations, null);
			} else {
				for (int i= 0, length= fOverrideAnnotations.length; i < length; i++)
					fAnnotationModel.removeAnnotation(fOverrideAnnotations[i]);
			}
			fOverrideAnnotations= null;
		}
	}

	/**
	 * Tells whether override indicators are shown.
	 * 
	 * @return <code>true</code> if the override indicators are shown
	 */
	private boolean showOverrideIndicators() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(fAnnotationPreference.getHighlightPreferenceKey())
			|| store.getBoolean(fAnnotationPreference.getVerticalRulerPreferenceKey())
			|| store.getBoolean(fAnnotationPreference.getOverviewRulerPreferenceKey())
			|| store.getBoolean(fAnnotationPreference.getTextPreferenceKey());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled()
	 */
	public void aboutToBeReconciled() {
		fCancelled= false;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(org.eclipse.jdt.core.dom.CompilationUnit, boolean, boolean)
	 */
	public void reconciled(CompilationUnit ast, boolean cancelled, boolean forced) {
		if (cancelled)
			return;
		updateAnnotations(ast);
	}
}

