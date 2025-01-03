/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.GuardedPattern;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.ISemanticTokensProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager.HighlightedPosition;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager.Highlighting;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings.DeprecatedMemberHighlighting;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings.RestrictedIdentifiersHighlighting;
import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;
import org.eclipse.jdt.internal.ui.util.ASTHelper;

/**
 * Semantic highlighting reconciler - Background thread implementation.
 *
 * @since 3.0
 */
public class SemanticHighlightingReconciler implements IJavaReconcilingListener, ITextInputListener {

	/**
	 * Collects positions from the AST.
	 */
	private class PositionCollector extends PositionCollectorCore {

		/** The semantic token */
		private SemanticToken fToken= new SemanticToken();


		@Override
		protected boolean visitLiteral(Expression node) {
			fToken.update(node);
			for (int i= 0, n= fJobSemanticHighlightings.length; i < n; i++) {
				SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
				if (fJobHighlightings[i].isEnabled() && semanticHighlighting.consumesLiteral(fToken)) {
					int offset= node.getStartPosition();
					int length= node.getLength();
					if (offset > -1 && length > 0)
						addPosition(offset, length, fJobHighlightings[i]);
					break;
				}
			}
			fToken.clear();
			return false;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.ConstructorInvocation)
		 * @since 3.5
		 */
		@Override
		public boolean visit(ConstructorInvocation node) {
			// XXX Hack for performance reasons (should loop over fJobSemanticHighlightings can call consumes(*))
			if (fJobDeprecatedMemberHighlighting != null) {
				IMethodBinding constructorBinding= node.resolveConstructorBinding();
				if (constructorBinding != null && constructorBinding.isDeprecated()) {
					int offset= node.getStartPosition();
					int length= 4;
					if (offset > -1 && length > 0)
						addPosition(offset, length, fJobDeprecatedMemberHighlighting);
				}
			}
			return true;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.ConstructorInvocation)
		 * @since 3.5
		 */
		@Override
		public boolean visit(SuperConstructorInvocation node) {
			// XXX Hack for performance reasons (should loop over fJobSemanticHighlightings can call consumes(*))
			if (fJobDeprecatedMemberHighlighting != null) {
				IMethodBinding constructorBinding= node.resolveConstructorBinding();
				if (constructorBinding != null && constructorBinding.isDeprecated()) {
					int offset= node.getStartPosition();
					int length= 5;
					if (offset > -1 && length > 0)
						addPosition(offset, length, fJobDeprecatedMemberHighlighting);
				}
			}
			return true;
		}

		@Override
		public boolean visit(SimpleType node) {
			if (node.getAST().apiLevel() >= ASTHelper.JLS10 && node.isVar()) {
				int offset= node.getStartPosition();
				int length= node.getLength();
				if (offset > -1 && length > 0) {
					for (int i= 0; i < fJobSemanticHighlightings.length; i++) {
						SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
						if (semanticHighlighting instanceof RestrictedIdentifiersHighlighting) {
							addPosition(offset, length, fJobHighlightings[i]);
							return false;
						}
					}
				}
			}
			return true;
		}

		@Override
		public boolean visit(YieldStatement node) {
			AST ast= node.getAST();
			if (ASTHelper.isYieldNodeSupportedInAST(ast) && !node.isImplicit()) {
				int offset= node.getStartPosition();
				int length= 5; // length of 'yield'
				if (offset > -1 && length > 0) {
					for (int i= 0; i < fJobSemanticHighlightings.length; i++) {
						SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
						if (semanticHighlighting instanceof RestrictedIdentifiersHighlighting) {
							addPosition(offset, length, fJobHighlightings[i]);
							return true;
						}
					}
				}
			}
			return true;
		}

		@Override
		public boolean visit(RecordDeclaration node) {
			AST ast= node.getAST();
			if (ASTHelper.isRecordDeclarationNodeSupportedInAST(ast)) {
				int offset= node.getRestrictedIdentifierStartPosition();
				int length= 6; // length of 'record'
				if (offset > -1 && length > 0) {
					for (int i= 0; i < fJobSemanticHighlightings.length; i++) {
						SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
						if (semanticHighlighting instanceof RestrictedIdentifiersHighlighting) {
							addPosition(offset, length, fJobHighlightings[i]);
							return true;
						}
					}
				}
			}
			return true;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			try {
				if (node.permittedTypes().size() > 0) {
					int offset= node.getRestrictedIdentifierStartPosition();
					int length= 7; // length of 'permits'
					if (offset > -1) {
						for (int i= 0; i < fJobSemanticHighlightings.length; i++) {
							SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
							if (semanticHighlighting instanceof RestrictedIdentifiersHighlighting) {
								addPosition(offset, length, fJobHighlightings[i]);
								return true;
							}
						}
					}
				}
			} catch (UnsupportedOperationException e) {
				// do nothing
			}
			return true;
		}

		@Override
		public boolean visit(Modifier node) {
			AST ast= node.getAST();
			int offset= node.getStartPosition();
			int length = 0;
			ModifierKeyword keyword= node.getKeyword();
			if (ASTHelper.isSealedTypeSupportedInAST(ast)) {
				if (keyword == ModifierKeyword.SEALED_KEYWORD) {
					length= 6;
				} else if (keyword == ModifierKeyword.NON_SEALED_KEYWORD) {
					length= 10;
				}
			}
			if (length == 0 && ast.apiLevel() >= AST.JLS23) {
				if (keyword == ModifierKeyword.MODULE_KEYWORD) {
					length= 6;
				}
			}
			if (offset > -1 && length > 0) {
				for (int i= 0; i < fJobSemanticHighlightings.length; i++) {
					SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
					if (semanticHighlighting instanceof RestrictedIdentifiersHighlighting) {
						addPosition(offset, length, fJobHighlightings[i]);
						return false;
					}
				}
			}
			return true;
		}

		/*
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SimpleName)
		 */
		@Override
		public boolean visit(SimpleName node) {
			fToken.update(node);
			for (int i= 0, n= fJobSemanticHighlightings.length; i < n; i++) {
				SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
				if (fJobHighlightings[i].isEnabled() && semanticHighlighting.consumes(fToken)) {
					int offset= node.getStartPosition();
					int length= node.getLength();
					if (offset > -1 && length > 0)
						addPosition(offset, length, fJobHighlightings[i]);
					break;
				}
			}
			fToken.clear();
			return false;
		}

		/**
		 * Retain the positions completely contained in the given range.
		 * @param offset The range offset
		 * @param length The range length
		 */
		@Override
		protected void retainPositions(int offset, int length) {
			// TODO: use binary search
			for (int i= 0, n= fRemovedPositions.size(); i < n; i++) {
				HighlightedPosition position= (HighlightedPosition) fRemovedPositions.get(i);
				if (position != null && position.isContained(offset, length)) {
					fRemovedPositions.set(i, null);
					fNOfRemovedPositions--;
				}
			}
		}

		@Override
		public boolean visit(GuardedPattern node) {
			try {
				if (node != null) {
					int offset= node.getRestrictedIdentifierStartPosition();
					int length= 4; // length of 'when'
					if (offset > -1) {
						for (int i= 0; i < fJobSemanticHighlightings.length; i++) {
							SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
							if (semanticHighlighting instanceof RestrictedIdentifiersHighlighting) {
								addPosition(offset, length, fJobHighlightings[i]);
								return true;
							}
						}
					}
				}
			} catch (UnsupportedOperationException e) {
				// do nothing
			}
			return true;
		}
	}

	/**
	 * Add a position with the given range and highlighting iff it does not exist already.
	 * @param offset The range offset
	 * @param length The range length
	 * @param highlighting The highlighting
	 */
	private void addPosition(int offset, int length, Highlighting highlighting) {
		boolean isExisting= false;
		// TODO: use binary search
		for (int i= 0, n= fRemovedPositions.size(); i < n; i++) {
			HighlightedPosition position= (HighlightedPosition) fRemovedPositions.get(i);
			if (position == null)
				continue;
			if (position.isEqual(offset, length, highlighting)) {
				isExisting= true;
				fRemovedPositions.set(i, null);
				fNOfRemovedPositions--;
				break;
			}
		}

		if (!isExisting) {
			Position position= fJobPresenter.createHighlightedPosition(offset, length, highlighting);
			fAddedPositions.add(position);
		}
	}

	private static final String JAVA_EDITOR_SEMANTIC_TOKENS_EXTENSION_POINT= "org.eclipse.jdt.ui.semanticTokens"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

	private static ISemanticTokensProvider[] fSemanticTokensProviders;

	/** Position collector */
	private PositionCollector fCollector= new PositionCollector();

	/** The Java editor this semantic highlighting reconciler is installed on */
	private JavaEditor fEditor;
	/** The source viewer this semantic highlighting reconciler is installed on */
	private ISourceViewer fSourceViewer;
	/** The semantic highlighting presenter */
	private SemanticHighlightingPresenter fPresenter;
	/** Semantic highlightings */
	private SemanticHighlighting[] fSemanticHighlightings;
	/** Highlightings */
	private Highlighting[] fHighlightings;
	/** Syntax Highlightings */
	private Highlighting[] fSyntaxHighlightings;

	/** Background job's added highlighted positions */
	private List<Position> fAddedPositions= new ArrayList<>();
	/** Background job's removed highlighted positions */
	private List<Position> fRemovedPositions= new ArrayList<>();
	/** Number of removed positions */
	private int fNOfRemovedPositions;

	/** Background job */
	private Job fJob;
	/** Background job lock */
	private final Object fJobLock= new Object();
	/**
	 * Reconcile operation lock.
	 * @since 3.2
	 */
	private final Object fReconcileLock= new Object();
	/**
	 * <code>true</code> if any thread is executing
	 * <code>reconcile</code>, <code>false</code> otherwise.
	 * @since 3.2
	 */
	private boolean fIsReconciling= false;

	/** The semantic highlighting presenter - cache for background thread, only valid during {@link #reconciled(CompilationUnit, boolean, IProgressMonitor)} */
	private SemanticHighlightingPresenter fJobPresenter;
	/** Semantic highlightings - cache for background thread, only valid during {@link #reconciled(CompilationUnit, boolean, IProgressMonitor)} */
	private SemanticHighlighting[] fJobSemanticHighlightings;
	/** Highlightings - cache for background thread, only valid during {@link #reconciled(CompilationUnit, boolean, IProgressMonitor)} */
	private Highlighting[] fJobHighlightings;
	private Highlighting[] fJobSyntaxHighlightings;

	/**
	 * XXX Hack for performance reasons (should loop over fJobSemanticHighlightings can call consumes(*))
	 * @since 3.5
	 */
	private Highlighting fJobDeprecatedMemberHighlighting;

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled()
	 */
	@Override
	public void aboutToBeReconciled() {
		// Do nothing
	}

	private static synchronized ISemanticTokensProvider[] getContributedSemanticTokensProviders() {
		if (fSemanticTokensProviders == null) {
			IExtensionRegistry registry= Platform.getExtensionRegistry();
			IConfigurationElement[] elements= registry.getConfigurationElementsFor(JAVA_EDITOR_SEMANTIC_TOKENS_EXTENSION_POINT);
			fSemanticTokensProviders = Arrays.stream(elements).map(ce -> {
				try {
					return (ISemanticTokensProvider) ce.createExecutableExtension(ATTR_CLASS);
				} catch (Exception e) {
					JavaPlugin.getDefault().getLog().error("Cannot instatiate semantic tokens provider '%s' contributed by '%s'".formatted(ce.getAttribute(ATTR_CLASS), ce.getContributor().getName()), e); //$NON-NLS-1$
					return null;
				}
			}).filter(Objects::nonNull).toArray(ISemanticTokensProvider[]::new);
		}
		return fSemanticTokensProviders;
	}

	private List<ISemanticTokensProvider.SemanticToken> getContributedSemanticTokens(CompilationUnit ast) {
		List<ISemanticTokensProvider.SemanticToken> contributedTokens = new ArrayList<>();
		for (ISemanticTokensProvider provider : getContributedSemanticTokensProviders()) {
			contributedTokens.addAll(provider.computeSemanticTokens(ast));
		}
		return contributedTokens;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(CompilationUnit, boolean, IProgressMonitor)
	 */
	@Override
	public void reconciled(CompilationUnit ast, boolean forced, IProgressMonitor progressMonitor) {
		// ensure at most one thread can be reconciling at any time
		synchronized (fReconcileLock) {
			if (fIsReconciling)
				return;
			else
				fIsReconciling= true;
		}
		fJobPresenter= fPresenter;
		fJobSemanticHighlightings= fSemanticHighlightings;
		fJobHighlightings= fHighlightings;
		fJobSyntaxHighlightings = fSyntaxHighlightings;

		try {
			if (fJobPresenter == null || fJobSemanticHighlightings == null || fJobHighlightings == null)
				return;

			fJobPresenter.setCanceled(progressMonitor.isCanceled());

			if (ast == null || fJobPresenter.isCanceled())
				return;

			ASTNode[] subtrees= getAffectedSubtrees(ast);
			if (subtrees.length == 0)
				return;

			startReconcilingPositions();

			if (!fJobPresenter.isCanceled()) {
				fJobDeprecatedMemberHighlighting= null;
				for (int i= 0, n= fJobSemanticHighlightings.length; i < n; i++) {
					SemanticHighlighting semanticHighlighting= fJobSemanticHighlightings[i];
					if (fJobHighlightings[i].isEnabled() && semanticHighlighting instanceof DeprecatedMemberHighlighting) {
						fJobDeprecatedMemberHighlighting= fJobHighlightings[i];
						break;
					}
				}

				reconcilePositions(subtrees, getContributedSemanticTokens(ast));

			}

			TextPresentation textPresentation= null;
			if (!fJobPresenter.isCanceled())
				textPresentation= fJobPresenter.createPresentation(fAddedPositions, fRemovedPositions);

			if (!fJobPresenter.isCanceled())
				updatePresentation(textPresentation, fAddedPositions, fRemovedPositions);

			stopReconcilingPositions();
		} finally {
			fJobPresenter= null;
			fJobSemanticHighlightings= null;
			fJobHighlightings= null;
			fJobDeprecatedMemberHighlighting= null;
			fJobSyntaxHighlightings = null;
			synchronized (fReconcileLock) {
				fIsReconciling= false;
			}
		}
	}

	/**
	 * @param node Root node
	 * @return Array of subtrees that may be affected by past document changes
	 */
	private ASTNode[] getAffectedSubtrees(ASTNode node) {
		// TODO: only return nodes which are affected by document changes - would require an 'anchor' concept for taking distant effects into account
		return new ASTNode[] { node };
	}

	/**
	 * Start reconciling positions.
	 */
	private void startReconcilingPositions() {
		fJobPresenter.addAllPositions(fRemovedPositions);
		fNOfRemovedPositions= fRemovedPositions.size();
	}

	/**
	 * Reconcile positions based on the AST subtrees
	 *
	 * @param subtrees the AST subtrees
	 * @param contributedTokens contributed semantic tokens data
	 */
	private void reconcilePositions(ASTNode[] subtrees, List<ISemanticTokensProvider.SemanticToken> contributedTokens) {
		// FIXME: remove positions not covered by subtrees

		for (ASTNode subtree : subtrees)
			subtree.accept(fCollector);

		for (ISemanticTokensProvider.SemanticToken t : contributedTokens) {
			Highlighting h = fromSemanticTokenType(t.tokenType());
			if (h == null) {
				JavaPlugin.logErrorMessage("Cannot find semantic highlighting for %s".formatted(t)); //$NON-NLS-1$
			} else {
				addPosition(t.ofset(), t.length(), h);
			}
		}
		List<Position> oldPositions= fRemovedPositions;
		List<Position> newPositions= new ArrayList<>(fNOfRemovedPositions);
		for (Position current : oldPositions) {
			if (current != null)
				newPositions.add(current);
		}
		fRemovedPositions= newPositions;
	}

	private Highlighting fromSemanticTokenType(ISemanticTokensProvider.TokenType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
			case OPERATOR:
				return findSyntaxHighlighting(PreferenceConstants.EDITOR_JAVA_OPERATOR_COLOR);
			case SINGLE_LINE_COMMENT:
				return findSyntaxHighlighting(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR);
			case KEYWORD:
				return findSyntaxHighlighting(PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR);
			case BRACKET:
				return findSyntaxHighlighting(PreferenceConstants.EDITOR_JAVA_BRACKET_COLOR);
			case MULTI_LINE_COMMENT:
				return findSyntaxHighlighting(PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR);
			case STRING:
				return findSyntaxHighlighting(PreferenceConstants.EDITOR_STRING_COLOR);
			case METHOD:
				return findSemanticHighlighting(SemanticHighlightings.METHOD);
			case ABSTRACT_CLASS:
				return findSemanticHighlighting(SemanticHighlightings.ABSTRACT_CLASS);
			case ABSTRACT_METHOD_INVOCATION:
				return findSemanticHighlighting(SemanticHighlightings.ABSTRACT_METHOD_INVOCATION);
			case ANNOTATION:
				return findSemanticHighlighting(SemanticHighlightings.ANNOTATION);
			case ANNOTATION_ELEMENT_REFERENCE:
				return findSemanticHighlighting(SemanticHighlightings.ANNOTATION_ELEMENT_REFERENCE);
			case AUTOBOXING:
				return findSemanticHighlighting(SemanticHighlightings.AUTOBOXING);
			case CLASS:
				return findSemanticHighlighting(SemanticHighlightings.CLASS);
			case DEPRECATED_MEMBER:
				return findSemanticHighlighting(SemanticHighlightings.DEPRECATED_MEMBER);
			case ENUM:
				return findSemanticHighlighting(SemanticHighlightings.ENUM);
			case FIELD:
				return findSemanticHighlighting(SemanticHighlightings.FIELD);
			case INHERITED_FIELD:
				return findSemanticHighlighting(SemanticHighlightings.INHERITED_FIELD);
			case INHERITED_METHOD_INVOCATION:
				return findSemanticHighlighting(SemanticHighlightings.INHERITED_METHOD_INVOCATION);
			case INTERFACE:
				return findSemanticHighlighting(SemanticHighlightings.INTERFACE);
			case LOCAL_VARIABLE:
				return findSemanticHighlighting(SemanticHighlightings.LOCAL_VARIABLE);
			case LOCAL_VARIABLE_DECLARATION:
				return findSemanticHighlighting(SemanticHighlightings.LOCAL_VARIABLE_DECLARATION);
			case METHOD_DECLARATION:
				return findSemanticHighlighting(SemanticHighlightings.METHOD_DECLARATION);
			case NUMBER:
				return findSemanticHighlighting(SemanticHighlightings.NUMBER);
			case PARAMETER_VARIABLE:
				return findSemanticHighlighting(SemanticHighlightings.PARAMETER_VARIABLE);
			case STATIC_FIELD:
				return findSemanticHighlighting(SemanticHighlightings.STATIC_FIELD);
			case STATIC_FINAL_FIELD:
				return findSemanticHighlighting(SemanticHighlightings.STATIC_FINAL_FIELD);
			case STATIC_METHOD_INVOCATION:
				return findSemanticHighlighting(SemanticHighlightings.STATIC_METHOD_INVOCATION);
			case TYPE_ARGUMENT:
				return findSemanticHighlighting(SemanticHighlightings.TYPE_ARGUMENT);
			case TYPE_VARIABLE:
				return findSemanticHighlighting(SemanticHighlightings.TYPE_VARIABLE);
			case RESTRICTED_IDENTIFIER:
				return findSemanticHighlighting(SemanticHighlightingsCore.RESTRICTED_KEYWORDS);
			case DEFAULT:
			default:
				return findSyntaxHighlighting(PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR);
		}
	}

	private Highlighting findSemanticHighlighting(String preferenceKey) {
		for (Highlighting h : fJobHighlightings) {
			if (Objects.equals(preferenceKey, h.getPreferenceKey())) {
				if (!h.isEnabled()) {
					break;
				}
				return h;
			}
		}
		return null;
	}

	private Highlighting findSyntaxHighlighting(String key) {
		for (Highlighting h : fJobSyntaxHighlightings) {
			if (Objects.equals(key, h.getPreferenceKey())) {
				if (!h.isEnabled()) {
					break;
				}
				return h;
			}
		}
		return null;
	}

	/**
	 * Update the presentation.
	 *
	 * @param textPresentation the text presentation
	 * @param addedPositions the added positions
	 * @param removedPositions the removed positions
	 */
	private void updatePresentation(TextPresentation textPresentation, List<Position> addedPositions, List<Position> removedPositions) {
		Runnable runnable= fJobPresenter.createUpdateRunnable(textPresentation, addedPositions, removedPositions);
		if (runnable == null)
			return;

		JavaEditor editor= fEditor;
		if (editor == null)
			return;

		IWorkbenchPartSite site= editor.getSite();
		if (site == null)
			return;

		Shell shell= site.getShell();
		if (shell == null || shell.isDisposed())
			return;

		Display display= shell.getDisplay();
		if (display == null || display.isDisposed())
			return;

		IDocument document= fSourceViewer.getDocument();
		display.asyncExec(() -> {
			// check Editor not reused otherwise meanwhile
			if (fSourceViewer != null && document == fSourceViewer.getDocument()) {
				runnable.run();
			}
		});
	}

	/**
	 * Stop reconciling positions.
	 */
	private void stopReconcilingPositions() {
		fRemovedPositions.clear();
		fNOfRemovedPositions= 0;
		fAddedPositions.clear();
	}

	/**
	 * Install this reconciler on the given editor, presenter and highlightings.
	 * @param editor the editor
	 * @param sourceViewer the source viewer
	 * @param presenter the semantic highlighting presenter
	 * @param semanticHighlightings the semantic highlightings
	 * @param highlightings the highlightings
	 * @param syntaxHighlightings editor syntax highlightings
	 */
	public void install(JavaEditor editor, ISourceViewer sourceViewer, SemanticHighlightingPresenter presenter, SemanticHighlighting[] semanticHighlightings, Highlighting[] highlightings, Highlighting[] syntaxHighlightings) {
		fPresenter= presenter;
		fSemanticHighlightings= semanticHighlightings;
		fHighlightings= highlightings;
		fSyntaxHighlightings= syntaxHighlightings;

		fEditor= editor;
		fSourceViewer= sourceViewer;

		if (fEditor instanceof CompilationUnitEditor) {
			if (registerAsEditorReconcilingListener()) {
				((CompilationUnitEditor)fEditor).addReconcileListener(this);
			}
		} else if (fEditor != null) {
			if (registerAsSourceViewerTextInputListener()) {
				fSourceViewer.addTextInputListener(this);
			}
			scheduleJob();
		}
	}

	/**
	 * Decides if this reconciler should also register itself as a reconciling listener on the editor as part of {@link #install} process.
	 * @return whether this instance should register itself as a reconciling listener on the editor
	 */
	protected boolean registerAsEditorReconcilingListener() {
		return true;
	}

	/**
	 * Decides if this reconciler should also register itself as a text input listener on the source viewer as part of {@link #install} process.
	 * @return whether this instance should register itself as a text input listener on the source viewer
	 */
	protected boolean registerAsSourceViewerTextInputListener() {
		return true;
	}

	/**
	 * Uninstall this reconciler from the editor
	 */
	public void uninstall() {
		if (fPresenter != null)
			fPresenter.setCanceled(true);

		if (fEditor != null) {
			if (fEditor instanceof CompilationUnitEditor)
				((CompilationUnitEditor)fEditor).removeReconcileListener(this);
			else
				fSourceViewer.removeTextInputListener(this);
			fEditor= null;
		}

		fSourceViewer= null;
		fSemanticHighlightings= null;
		fHighlightings= null;
		fPresenter= null;
	}

	/**
	 * Get the type root java element for which reconciling job should be scheduled.
	 *
	 * @return root element for reconciling
	 */
	protected ITypeRoot getElement() {
		return fEditor.getInputJavaElement();
	}

	/**
	 * Schedule a background job for retrieving the AST and reconciling the Semantic Highlighting model.
	 */
	private void scheduleJob() {
		final ITypeRoot element= getElement();

		synchronized (fJobLock) {
			final Job oldJob= fJob;
			if (fJob != null) {
				fJob.cancel();
				fJob= null;
			}

			if (element != null) {
				fJob= new Job(JavaEditorMessages.SemanticHighlighting_job) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						if (oldJob != null) {
							try {
								oldJob.join();
							} catch (InterruptedException e) {
								JavaPlugin.log(e);
								return Status.CANCEL_STATUS;
							}
						}
						if (monitor.isCanceled())
							return Status.CANCEL_STATUS;
						JavaCore.runReadOnly(() -> {
							CompilationUnit ast= SharedASTProviderCore.getAST(element, SharedASTProviderCore.WAIT_YES, monitor);
							reconciled(ast, false, monitor);
						});
						synchronized (fJobLock) {
							// allow the job to be gc'ed
							if (fJob == this)
								fJob= null;
						}
						return Status.OK_STATUS;
					}
				};
				fJob.setSystem(true);
				fJob.setPriority(Job.DECORATE);
				fJob.schedule();
			}
		}
	}

	/*
	 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
	 */
	@Override
	public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
		synchronized (fJobLock) {
			if (fJob != null) {
				fJob.cancel();
				fJob= null;
			}
		}
	}

	/*
	 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
	 */
	@Override
	public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
		if (newInput != null)
			scheduleJob();
	}

	/**
	 * Refreshes the highlighting.
	 *
	 * @since 3.2
	 */
	public void refresh() {
		scheduleJob();
	}
}
