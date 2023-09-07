package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.UsesDirective;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;

import org.eclipse.jdt.internal.ui.JavaUIStatus;

public class AddMissingModuleJavadocTagProposalCore extends CUCorrectionProposalCore {

	private final ModuleDeclaration fDecl; // MethodDecl or TypeDecl or ModuleDecl
	private final ASTNode fMissingNode;
	public AddMissingModuleJavadocTagProposalCore(String label, ICompilationUnit cu, ModuleDeclaration decl, ASTNode missingNode, int relevance) {
		super(label, cu, null, relevance);
		fDecl= decl;
		fMissingNode= missingNode;
	}

	@Override
	public void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
		try {
			Javadoc javadoc= null;
			String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
			final ICompilationUnit unit= getCompilationUnit();
			CompilationUnit cu= (CompilationUnit)fDecl.getParent();
			Name name= fDecl.getName();
			List<Comment> comments= cu.getCommentList();
			for (Comment comment : comments) {
				if (comment instanceof Javadoc
						&& comment.getStartPosition() + comment.getLength() < name.getStartPosition()) {
					javadoc= (Javadoc)comment;
				}
			}
			if (javadoc == null) {
				return;
			}
			StringBuilder comment= new StringBuilder();
			int insertPosition= findInsertPosition(javadoc, fMissingNode, document, lineDelimiter);

		 	if (fMissingNode instanceof UsesDirective) {
		 		UsesDirective directive= (UsesDirective)fMissingNode;
		 		comment.append(" * ").append(TagElement.TAG_USES).append(" ").append(directive.getName().getFullyQualifiedName().toString()).append(lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
		 	} else if (fMissingNode instanceof ProvidesDirective) {
		 		ProvidesDirective directive= (ProvidesDirective)fMissingNode;
		 		comment.append(" * ").append(TagElement.TAG_PROVIDES).append(" ").append(directive.getName().getFullyQualifiedName().toString()).append(lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
		 	}
			IRegion region= document.getLineInformationOfOffset(insertPosition);

			String lineContent= document.get(region.getOffset(), region.getLength());
			String indentString= Strings.getIndentString(lineContent, unit);
			String str= Strings.changeIndent(comment.toString(), 0, unit, indentString, lineDelimiter);
			InsertEdit edit= new InsertEdit(insertPosition, str);
			rootEdit.addChild(edit);
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}
	}

	public static int findInsertPosition(Javadoc javadoc, ASTNode node, IDocument document, String lineDelimiter) throws BadLocationException {
		int position= -1;
		List<TagElement> tags= javadoc.tags();
		TagElement lastTag= null;
		// Put new tag after last @provides tag if found
		for (TagElement tag : tags) {
			String name= tag.getTagName();
			if (TagElement.TAG_PROVIDES.equals(name)) {
				lastTag= tag;
			}
		}
		if (lastTag == null) {
			// otherwise add before first @uses tag
			for (TagElement tag : tags) {
				String name= tag.getTagName();
				if (TagElement.TAG_USES.equals(name)) {
					IRegion region= document.getLineInformationOfOffset(tag.getStartPosition());
					return region.getOffset();
				}
			}
		}
		// otherwise put after last tag
		if (lastTag == null && !tags.isEmpty()) {
			lastTag= tags.get(tags.size() - 1);
		}
		if (lastTag != null) {
			IRegion region= document.getLineInformationOfOffset(lastTag.getStartPosition());
			position= region.getOffset() + region.getLength() + lineDelimiter.length();
		} else {
			// otherwise put after javadoc comment start
			IRegion region= document.getLineInformationOfOffset(javadoc.getStartPosition());
			position= region.getOffset() + region.getLength() + lineDelimiter.length();
		}
		return position;
	}
}