/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.preferences.formatter.SnippetPreview.PreviewSnippet;


/**
 * Manage code formatter white space options on a higher level. 
 */
public final class WhiteSpaceOptions {

    /**
     * Represents a node in the options tree.
     */
	public abstract static class Node {
	    
	    private final InnerNode fParent;
	    private final String fName;
	    
	    public int index;
	    
	    protected final Map fWorkingValues;
	    protected final ArrayList fChildren;

	    public Node(InnerNode parent, Map workingValues, String messageKey) {
	        if (workingValues == null || messageKey == null)
	            throw new IllegalArgumentException();
	        fParent= parent;
	        fWorkingValues= workingValues;
	        fName= FormatterMessages.getString(messageKey);
	        fChildren= new ArrayList();
	        if (fParent != null)
	            fParent.add(this);
	    }
	    
	    public abstract void setChecked(boolean checked);

	    public boolean hasChildren() { 
	        return !fChildren.isEmpty();
	    }
	    
	    public List getChildren() {
	        return Collections.unmodifiableList(fChildren);
	    }
	    
	    public InnerNode getParent() {
	        return fParent;
	    }

	    public final String toString() {
	        return fName;
	    }
	    
	    public abstract List getSnippets();
	    
	    public abstract void getCheckedLeafs(List list);
	}
	
	/**
	 * A node representing a group of options in the tree.
	 */
	public static class InnerNode extends Node {
	    
        public InnerNode(InnerNode parent, Map workingValues, String messageKey) {
            super(parent, workingValues, messageKey);
        }

	    public void setChecked(boolean checked) {
	        for (final Iterator iter = fChildren.iterator(); iter.hasNext();)
	            ((Node)iter.next()).setChecked(checked);
	    }

	    public void add(Node child) {
	        fChildren.add(child);
	    }

        public List getSnippets() {
            final ArrayList snippets= new ArrayList(fChildren.size());
            for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
                final List childSnippets= ((Node)iter.next()).getSnippets();
                for (final Iterator chIter= childSnippets.iterator(); chIter.hasNext(); ) {
                    final Object snippet= chIter.next();
                    if (!snippets.contains(snippet)) 
                        snippets.add(snippet);
                }
            }
            return snippets;
        }
        
        public void getCheckedLeafs(List list) {
            for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
                ((Node)iter.next()).getCheckedLeafs(list);
            }
        }
	}

	
	/**
	 * A node representing a concrete white space option in the tree.
	 */
	public static class OptionNode extends Node {
	    private final String fKey;
	    private final ArrayList fSnippets;
	    
	    public OptionNode(InnerNode parent, Map workingValues, String messageKey, String key, PreviewSnippet snippet) {
	        super(parent, workingValues, messageKey);
	        fKey= key;
	        fSnippets= new ArrayList(1);
	        fSnippets.add(snippet);
	    }
	    
        public void setChecked(boolean checked) {
        	fWorkingValues.put(fKey, checked ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
        }
        
        public boolean getChecked() {
            return JavaCore.INSERT.equals(fWorkingValues.get(fKey));
        }
        
        public List getSnippets() {
            return fSnippets;
        }
        
        public void getCheckedLeafs(List list) {
            if (getChecked()) 
                list.add(this);
        }
	}
	
	
	
	/**
	 * Preview snippets.
	 */
	
    private final static PreviewSnippet FOR_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "for (int i= 0, j= array.length; i < array.length; i++, j--) {}" //$NON-NLS-1$
    );

    private final static PreviewSnippet WHILE_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "while (condition) {}; do {} while (condition);" //$NON-NLS-1$
    );

    private final static PreviewSnippet CATCH_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "try { number= Integer.parseInt(value); } catch (NumberFormatException e) {}"); //$NON-NLS-1$

    private final static PreviewSnippet IF_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "if (condition) { return foo; } else {return bar;}"); //$NON-NLS-1$

    private final static PreviewSnippet SYNCHRONIZED_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "synchronized (list) { list.add(element); }"); //$NON-NLS-1$

    private final static PreviewSnippet SWITCH_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "switch (number) { case RED: return GREEN; case GREEN: return BLUE; case BLUE: return RED; default: return BLACK;}"); //$NON-NLS-1$

    private final static PreviewSnippet CONSTRUCTOR_DECL_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_CLASS_BODY_DECLARATIONS, 
    "MyClass() throws E0, E1 { this(0,0,0);}" +  //$NON-NLS-1$
    "MyClass(int x, int y, int z) throws E0, E1 { super(x, y, z, true);}"); //$NON-NLS-1$

    private final static PreviewSnippet METHOD_DECL_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_CLASS_BODY_DECLARATIONS, 
    "void foo() throws E0, E1 {};" +  //$NON-NLS-1$
    "void bar(int x, int y) throws E0, E1 {}"); //$NON-NLS-1$

    private final static PreviewSnippet ARRAY_DECL_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "int [] array0= new int [] {};\n" +  //$NON-NLS-1$
    "int [] array1= new int [] {1, 2, 3};\n" +  //$NON-NLS-1$
    "int [] array2= new int[3];"); //$NON-NLS-1$

    private final static PreviewSnippet ARRAY_REF_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "array[i].foo();"); //$NON-NLS-1$

    private final static PreviewSnippet METHOD_CALL_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "foo();\n" +  //$NON-NLS-1$
    "bar(x, y);"); //$NON-NLS-1$

//    private final static PreviewSnippet CONSTR_CALL_PREVIEW= new PreviewSnippet(
//    CodeFormatter.K_STATEMENTS, 
//    "this();\n\n" +  //$NON-NLS-1$
//    "this(x, y);\n"); //$NON-NLS-1$

    private final static PreviewSnippet ALLOC_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "String str= new String(); Point point= new Point(x, y);"); //$NON-NLS-1$

    private final static PreviewSnippet LABEL_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "label: for (int i= 0; i<list.length; i++) {for (int j= 0; j < list[i].length; j++) continue label;}"); //$NON-NLS-1$

    private final static PreviewSnippet SEMICOLON_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "int a= 4; foo(); bar(x, y);"); //$NON-NLS-1$

    private final static PreviewSnippet CONDITIONAL_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "String value= condition ? TRUE : FALSE;"); //$NON-NLS-1$

    private final static PreviewSnippet CLASS_DECL_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_COMPILATION_UNIT, 
    "class MyClass implements I0, I1, I2 {}"); //$NON-NLS-1$

    private final static PreviewSnippet ANON_CLASS_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "AnonClass= new AnonClass() {void foo(Some s) { }};"); //$NON-NLS-1$

    private final static PreviewSnippet OPERATOR_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "List list= new ArrayList(); int a= -4 + -9; b= a++ / --number; c += 4; boolean value= true && false;"); //$NON-NLS-1$

    private final static PreviewSnippet CAST_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "String s= ((String)object);"); //$NON-NLS-1$

    private final static PreviewSnippet MULT_LOCAL_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "int a= 0, b= 1, c= 2, d= 3;"); //$NON-NLS-1$

    private final static PreviewSnippet MULT_FIELD_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_CLASS_BODY_DECLARATIONS, 
    "int a=0,b=1,c=2,d=3;"); //$NON-NLS-1$

    private final static PreviewSnippet BLOCK_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "if (true) { return 1; } else { return 2; }"); //$NON-NLS-1$

    private final static PreviewSnippet PAREN_EXPR_PREVIEW= new PreviewSnippet(
    CodeFormatter.K_STATEMENTS, 
    "result= (a *( b +  c + d) * (e + f));"); //$NON-NLS-1$

    //TODO: integrate this
//    private final static PreviewSnippet ASSERT_PREVIEW= new PreviewSnippet(
//        CodeFormatter.K_STATEMENTS,
//        "assert condition : reportError();"
//    );

    /**
     * Create the tree, in this order: position - syntax element - abstract
     * element
     */
    public static ArrayList createTreeByPosition(Map workingValues) {

        final ArrayList roots= new ArrayList();
        
        final InnerNode before= new InnerNode(null, workingValues, "WhiteSpaceOptions.before"); //$NON-NLS-1$
        createBeforeOpenParenTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.opening_paren")); //$NON-NLS-1$
        createBeforeClosingParenTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.closing_paren")); //$NON-NLS-1$
        createBeforeOpenBraceTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.opening_brace")); //$NON-NLS-1$
        createBeforeClosingBraceTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.closing_brace")); //$NON-NLS-1$
        createBeforeOpenBracketTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.opening_bracket")); //$NON-NLS-1$
        createBeforeClosingBracketTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.closing_bracket")); //$NON-NLS-1$
        createBeforeOperatorTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.operator")); //$NON-NLS-1$
        createBeforeCommaTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.comma")); //$NON-NLS-1$
        createBeforeColonTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.colon")); //$NON-NLS-1$
        createBeforeSemicolonTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.semicolon")); //$NON-NLS-1$
        createBeforeQuestionTree(workingValues, createChild(before, workingValues, "WhiteSpaceOptions.question_mark")); //$NON-NLS-1$
        roots.add(before);

        final InnerNode after= new InnerNode(null, workingValues, "WhiteSpaceOptions.after"); //$NON-NLS-1$
        createAfterOpenParenTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.opening_paren")); //$NON-NLS-1$
        createAfterCloseParenTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.closing_paren")); //$NON-NLS-1$
        createAfterOpenBraceTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.opening_brace")); //$NON-NLS-1$
        createAfterCloseBraceTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.closing_brace")); //$NON-NLS-1$
        createAfterOpenBracketTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.opening_bracket")); //$NON-NLS-1$
        createAfterOperatorTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.operator")); //$NON-NLS-1$
        createAfterCommaTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.comma")); //$NON-NLS-1$
        createAfterColonTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.colon")); //$NON-NLS-1$
        createAfterSemicolonTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.semicolon")); //$NON-NLS-1$
        createAfterQuestionTree(workingValues, createChild(after, workingValues, "WhiteSpaceOptions.question_mark")); //$NON-NLS-1$
        roots.add(after);

        final InnerNode between= new InnerNode(null, workingValues, "WhiteSpaceOptions.between"); //$NON-NLS-1$
        createBetweenEmptyBracesTree(workingValues, createChild(between, workingValues, "WhiteSpaceOptions.empty_braces")); //$NON-NLS-1$
        createBetweenEmptyBracketsTree(workingValues, createChild(between, workingValues, "WhiteSpaceOptions.empty_brackets")); //$NON-NLS-1$
        createBetweenEmptyParenTree(workingValues, createChild(between, workingValues, "WhiteSpaceOptions.empty_parens")); //$NON-NLS-1$
        roots.add(between);

        return roots;
	}
	
	/**
	 * Create the tree, in this order: syntax element - position - abstract element
	 */
	public static ArrayList createTreeBySyntaxElem(Map workingValues) {
        final ArrayList roots= new ArrayList();
        
        InnerNode element;

        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.opening_paren"); //$NON-NLS-1$
        createBeforeOpenParenTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterOpenParenTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.closing_paren"); //$NON-NLS-1$
        createBeforeClosingParenTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterCloseParenTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.opening_brace"); //$NON-NLS-1$
        createBeforeOpenBraceTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterOpenBraceTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.closing_brace"); //$NON-NLS-1$
        createBeforeClosingBraceTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterCloseBraceTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.opening_bracket");         //$NON-NLS-1$
        createBeforeOpenBracketTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterOpenBracketTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.closing_bracket");         //$NON-NLS-1$
        createBeforeClosingBracketTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.operator");         //$NON-NLS-1$
        createBeforeOperatorTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterOperatorTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.comma");         //$NON-NLS-1$
        createBeforeCommaTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterCommaTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.colon");         //$NON-NLS-1$
        createBeforeColonTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterColonTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.semicolon");         //$NON-NLS-1$
        createBeforeSemicolonTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterSemicolonTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.question_mark");         //$NON-NLS-1$
        createBeforeQuestionTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.before")); //$NON-NLS-1$
        createAfterQuestionTree(workingValues, createChild(element, workingValues, "WhiteSpaceOptions.after")); //$NON-NLS-1$
        roots.add(element);

        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.between_empty_parens");         //$NON-NLS-1$
        createBetweenEmptyParenTree(workingValues, element);
        roots.add(element);

        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.between_empty_braces");         //$NON-NLS-1$
        createBetweenEmptyBracesTree(workingValues, element);
        roots.add(element);
        
        element= new InnerNode(null, workingValues, "WhiteSpaceOptions.between_empty_brackets");         //$NON-NLS-1$
        createBetweenEmptyBracketsTree(workingValues, element);
        roots.add(element);

        return roots;
	}
	
    /**
     * Create the tree, in this order: position - syntax element - abstract
     * element
     */
    public static ArrayList createAltTree(Map workingValues) {

        final ArrayList roots= new ArrayList();
        
        InnerNode parent;
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_opening_paren"); //$NON-NLS-1$
        createBeforeOpenParenTree(workingValues, parent);

        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_opening_paren"); //$NON-NLS-1$
        createAfterOpenParenTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_closing_paren"); //$NON-NLS-1$
        createBeforeClosingParenTree(workingValues, parent); 
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_closing_paren"); //$NON-NLS-1$
        createAfterCloseParenTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.between_empty_parens"); //$NON-NLS-1$
        createBetweenEmptyParenTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_opening_brace"); //$NON-NLS-1$
        createBeforeOpenBraceTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_opening_brace"); //$NON-NLS-1$
        createAfterOpenBraceTree(workingValues, parent);

        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_closing_brace"); //$NON-NLS-1$
        createBeforeClosingBraceTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_closing_brace"); //$NON-NLS-1$
        createAfterCloseBraceTree(workingValues, parent);

        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.between_empty_braces"); //$NON-NLS-1$
        createBetweenEmptyBracesTree(workingValues, parent);

        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_opening_bracket"); //$NON-NLS-1$
        createBeforeOpenBracketTree(workingValues, parent);

        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_opening_bracket"); //$NON-NLS-1$
        createAfterOpenBracketTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_closing_bracket"); //$NON-NLS-1$
        createBeforeClosingBracketTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.between_empty_brackets"); //$NON-NLS-1$
        createBetweenEmptyBracketsTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_operator"); //$NON-NLS-1$
        createBeforeOperatorTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_operator"); //$NON-NLS-1$
        createAfterOperatorTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_comma"); //$NON-NLS-1$
        createBeforeCommaTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_comma"); //$NON-NLS-1$
        createAfterCommaTree(workingValues, parent); 
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_colon"); //$NON-NLS-1$
        createAfterColonTree(workingValues, parent); 
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_colon"); //$NON-NLS-1$
        createBeforeColonTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_semicolon"); //$NON-NLS-1$
        createBeforeSemicolonTree(workingValues, parent);
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_semicolon"); //$NON-NLS-1$
        createAfterSemicolonTree(workingValues, parent); 
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.before_question_mark"); //$NON-NLS-1$
        createBeforeQuestionTree(workingValues, parent); 
        
        parent= createParentNode(roots, workingValues, "WhiteSpaceOptions.after_question_mark"); //$NON-NLS-1$
        createAfterQuestionTree(workingValues, parent); 
        
        return roots;
	}
    
    
    
    
    
    
    private static InnerNode createParentNode(List roots, Map workingValues, String text) {
        final InnerNode parent= new InnerNode(null, workingValues, text);
        roots.add(parent);
        return parent;
    }

    public static ArrayList createTreeByJavaElement(Map workingValues) {

        final InnerNode declarations= new InnerNode(null, workingValues, "WhiteSpaceTabPage.declarations"); //$NON-NLS-1$
        createClassTree(workingValues, declarations);
        createFieldTree(workingValues, declarations);
        createLocalVariableTree(workingValues, declarations);
        createConstructorTree(workingValues, declarations);
        createMethodDeclTree(workingValues, declarations);
        createLabelTree(workingValues, declarations);
        
        final InnerNode statements= new InnerNode(null, workingValues, "WhiteSpaceTabPage.statements"); //$NON-NLS-1$
        createOption(statements, workingValues, "WhiteSpaceOptions.before_semicolon", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON, SEMICOLON_PREVIEW); //$NON-NLS-1$
        createBlockTree(workingValues, statements);
        createIfStatementTree(workingValues, statements);
        createForStatementTree(workingValues, statements);
        createSwitchStatementTree(workingValues, statements);
        createDoWhileTree(workingValues, statements);
        createSynchronizedTree(workingValues, statements);
        createTryStatementTree(workingValues, statements);
        
        final InnerNode expressions= new InnerNode(null, workingValues, "WhiteSpaceTabPage.expressions"); //$NON-NLS-1$
        createFunctionCallTree(workingValues, expressions);
		createAssignmentTree(workingValues, expressions);
		createOperatorTree(workingValues, expressions);
		createParenthesizedExpressionTree(workingValues, expressions);
		createTypecastTree(workingValues, expressions);
		createConditionalTree(workingValues, expressions);
		
		final InnerNode arrays= new InnerNode(null, workingValues, "WhiteSpaceTabPage.arrays"); //$NON-NLS-1$
		createArrayDeclarationTree(workingValues, arrays);
		createArrayAllocTree(workingValues, arrays);
		createArrayInitializerTree(workingValues, arrays);
		createArrayElementAccessTree(workingValues, arrays);
		
		
        final ArrayList roots= new ArrayList();
		roots.add(declarations);
		roots.add(statements);
		roots.add(expressions);
		roots.add(arrays);
        return roots;
    }
	
	private static void createBeforeQuestionTree(Map workingValues, final InnerNode parent) {

        createOption(parent, workingValues, "WhiteSpaceOptions.conditional", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL, CONDITIONAL_PREVIEW); //$NON-NLS-1$
    }

    private static void createBeforeSemicolonTree(Map workingValues, final InnerNode parent) {

        createOption(parent, workingValues, "WhiteSpaceOptions.for", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR, FOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.statements", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON, SEMICOLON_PREVIEW); //$NON-NLS-1$
    }

    private static void createBeforeColonTree(Map workingValues, final InnerNode parent) {
//TODO:        createOption(parent, workingValues, "WhiteSpaceOptions.assert", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT, ASSERT_PREVIEW);
        createOption(parent, workingValues, "WhiteSpaceOptions.conditional", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL, CONDITIONAL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.label", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT, LABEL_PREVIEW); //$NON-NLS-1$

        final InnerNode switchStatement= createChild(parent, workingValues, "WhiteSpaceOptions.switch"); //$NON-NLS-1$
        createOption(switchStatement, workingValues, "WhiteSpaceOptions.case", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE, SWITCH_PREVIEW); //$NON-NLS-1$
        createOption(switchStatement, workingValues, "WhiteSpaceOptions.default", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT, SWITCH_PREVIEW); //$NON-NLS-1$
    }

    private static void createBeforeCommaTree(Map workingValues, final InnerNode parent) {

        final InnerNode forStatement= createChild(parent, workingValues, "WhiteSpaceOptions.for");  //$NON-NLS-1$
        createOption(forStatement, workingValues, "WhiteSpaceOptions.initialization", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS, FOR_PREVIEW); //$NON-NLS-1$
        createOption(forStatement, workingValues, "WhiteSpaceOptions.incrementation", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS, FOR_PREVIEW); //$NON-NLS-1$
            
        final InnerNode invocation= createChild(parent, workingValues, "WhiteSpaceOptions.arguments");  //$NON-NLS-1$
        createOption(invocation, workingValues, "WhiteSpaceOptions.method_call", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(invocation, workingValues, "WhiteSpaceOptions.explicit_constructor_call", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(invocation, workingValues, "WhiteSpaceOptions.alloc_expr", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION, ALLOC_PREVIEW); //$NON-NLS-1$

        final InnerNode decl= createChild(parent, workingValues, "WhiteSpaceOptions.parameters"); //$NON-NLS-1$
        createOption(decl, workingValues, "WhiteSpaceOptions.constructor", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(decl, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS, METHOD_DECL_PREVIEW); //$NON-NLS-1$

        final InnerNode throwsDecl= createChild(parent, workingValues, "WhiteSpaceOptions.throws");  //$NON-NLS-1$
        createOption(throwsDecl, workingValues, "WhiteSpaceOptions.constructor", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(throwsDecl, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        
        final InnerNode multDecls= createChild(parent, workingValues, "WhiteSpaceOptions.mult_decls"); //$NON-NLS-1$
        createOption(multDecls, workingValues, "WhiteSpaceOptions.fields", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, MULT_FIELD_PREVIEW); //$NON-NLS-1$
        createOption(multDecls, workingValues, "WhiteSpaceOptions.local_vars", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, MULT_LOCAL_PREVIEW); //$NON-NLS-1$

        createOption(parent, workingValues, "WhiteSpaceOptions.initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.implements_clause", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES, CLASS_DECL_PREVIEW); //$NON-NLS-1$
    }

    private static void createBeforeOperatorTree(Map workingValues, final InnerNode parent) {
        createOption(parent, workingValues, "WhiteSpaceOptions.assignment_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.unary_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.binary_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.prefix_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.postfix_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
    }

    private static void createBeforeClosingBracketTree(Map workingValues, final InnerNode parent) {
        createOption(parent, workingValues, "WhiteSpaceOptions.array_alloc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.array_element_access", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE, ARRAY_REF_PREVIEW); //$NON-NLS-1$
    }

    private static void createBeforeOpenBracketTree(Map workingValues, final InnerNode parent) {
        createOption(parent, workingValues, "WhiteSpaceOptions.array_decl", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.array_alloc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.array_element_access", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE, ARRAY_REF_PREVIEW); //$NON-NLS-1$
    }

    private static void createBeforeClosingBraceTree(Map workingValues, final InnerNode parent) {
        createOption(parent, workingValues, "WhiteSpaceOptions.array_init", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, CLASS_DECL_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createBeforeOpenBraceTree(Map workingValues, final InnerNode parent) {

        createOption(parent, workingValues, "WhiteSpaceOptions.class_decl", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION, CLASS_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.anon_class_decl", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION, ANON_CLASS_PREVIEW); //$NON-NLS-1$

        final InnerNode functionDecl= createChild(parent, workingValues, "WhiteSpaceOptions.member_function_declaration"); { //$NON-NLS-1$
            createOption(functionDecl, workingValues, "WhiteSpaceOptions.constructor", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
            createOption(functionDecl, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        }
        
        createOption(parent, workingValues, "WhiteSpaceOptions.initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.block", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK, BLOCK_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.switch", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH, SWITCH_PREVIEW); //$NON-NLS-1$
    }

    private static void createBeforeClosingParenTree(Map workingValues, final InnerNode parent) {

        createOption(parent, workingValues, "WhiteSpaceOptions.catch", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH, CATCH_PREVIEW);  //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.for", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR, FOR_PREVIEW);  //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.if", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF, IF_PREVIEW);  //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.switch", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH, SWITCH_PREVIEW);  //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.synchronized", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED, SYNCHRONIZED_PREVIEW);  //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.while", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE, WHILE_PREVIEW); //$NON-NLS-1$
	
        createOption(parent, workingValues, "WhiteSpaceOptions.type_cast", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST, CAST_PREVIEW); //$NON-NLS-1$
		    
        final InnerNode decl= createChild(parent, workingValues, "WhiteSpaceOptions.member_function_declaration"); //$NON-NLS-1$
        createOption(decl, workingValues, "WhiteSpaceOptions.constructor", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(decl, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW);  //$NON-NLS-1$

        createOption(parent, workingValues, "WhiteSpaceOptions.method_call", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION, METHOD_CALL_PREVIEW);  //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.paren_expr", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION, PAREN_EXPR_PREVIEW);  //$NON-NLS-1$
    }

    private static void createBeforeOpenParenTree(Map workingValues, final InnerNode parent) {

        createOption(parent, workingValues, "WhiteSpaceOptions.catch", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH, CATCH_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.for", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR, FOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.if", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF, IF_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.switch", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH, SWITCH_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.synchronized", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED, SYNCHRONIZED_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.while", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE, WHILE_PREVIEW); //$NON-NLS-1$
        
        final InnerNode decls= createChild(parent, workingValues, "WhiteSpaceOptions.member_function_declaration");  //$NON-NLS-1$
        createOption(decls, workingValues, "WhiteSpaceOptions.constructor", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(decls, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        
        createOption(parent, workingValues, "WhiteSpaceOptions.method_call", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.paren_expr", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION, PAREN_EXPR_PREVIEW); //$NON-NLS-1$
    }

	private static void createAfterQuestionTree(Map workingValues, final InnerNode parent) {

        createOption(parent, workingValues, "WhiteSpaceOptions.conditional", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL, CONDITIONAL_PREVIEW); //$NON-NLS-1$
    }

    private static void createAfterSemicolonTree(Map workingValues, final InnerNode parent) {

        createOption(parent, workingValues, "WhiteSpaceOptions.for", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR, FOR_PREVIEW); //$NON-NLS-1$
    }

    private static void createAfterColonTree(Map workingValues, final InnerNode parent) {
//TODO:        createOption(parent, workingValues, "'assert'", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT, ASSERT_PREVIEW);
        createOption(parent, workingValues, "WhiteSpaceOptions.conditional", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL, CONDITIONAL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.label", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT, LABEL_PREVIEW); //$NON-NLS-1$
    }

    private static void createAfterCommaTree(Map workingValues, final InnerNode parent) {

        final InnerNode forStatement= createChild(parent, workingValues, "WhiteSpaceOptions.for"); { //$NON-NLS-1$
            createOption(forStatement, workingValues, "WhiteSpaceOptions.initialization", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS, FOR_PREVIEW); //$NON-NLS-1$
            createOption(forStatement, workingValues, "WhiteSpaceOptions.incrementation", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS, FOR_PREVIEW); //$NON-NLS-1$
        }
        final InnerNode invocation= createChild(parent, workingValues, "WhiteSpaceOptions.arguments"); { //$NON-NLS-1$
            createOption(invocation, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, METHOD_CALL_PREVIEW); //$NON-NLS-1$
            createOption(invocation, workingValues, "WhiteSpaceOptions.explicit_constructor_call", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
            createOption(invocation, workingValues, "WhiteSpaceOptions.alloc_expr", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION, ALLOC_PREVIEW); //$NON-NLS-1$
        }
        final InnerNode decl= createChild(parent, workingValues, "WhiteSpaceOptions.parameters"); { //$NON-NLS-1$
            createOption(decl, workingValues, "WhiteSpaceOptions.constructor", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
            createOption(decl, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        }
        final InnerNode throwsDecl= createChild(parent, workingValues, "WhiteSpaceOptions.throws"); { //$NON-NLS-1$
            createOption(throwsDecl, workingValues, "WhiteSpaceOptions.constructor", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
            createOption(throwsDecl, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        }
        final InnerNode multDecls= createChild(parent, workingValues, "WhiteSpaceOptions.mult_decls"); { //$NON-NLS-1$
            createOption(multDecls, workingValues, "WhiteSpaceOptions.fields", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, MULT_FIELD_PREVIEW); //$NON-NLS-1$
            createOption(multDecls, workingValues, "WhiteSpaceOptions.local_vars", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, MULT_LOCAL_PREVIEW); //$NON-NLS-1$
        }
        createOption(parent, workingValues, "WhiteSpaceOptions.initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.implements_clause", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES, CLASS_DECL_PREVIEW); //$NON-NLS-1$
    }

    private static void createAfterOperatorTree(Map workingValues, final InnerNode parent) {

        createOption(parent, workingValues, "WhiteSpaceOptions.assignment_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.unary_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.binary_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.prefix_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.postfix_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createAfterOpenBracketTree(Map workingValues, final InnerNode parent) {
        
        createOption(parent, workingValues, "WhiteSpaceOptions.array_alloc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.array_element_access", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE, ARRAY_REF_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createAfterOpenBraceTree(Map workingValues, final InnerNode parent) {
        
        createOption(parent, workingValues, "WhiteSpaceOptions.initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createAfterCloseBraceTree(Map workingValues, final InnerNode parent) {
        createOption(parent, workingValues, "WhiteSpaceOptions.block", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK, BLOCK_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createAfterCloseParenTree(Map workingValues, final InnerNode parent) {
        
        createOption(parent, workingValues, "WhiteSpaceOptions.type_cast", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, CAST_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createAfterOpenParenTree(Map workingValues, final InnerNode parent) {
        createOption(parent, workingValues, "WhiteSpaceOptions.catch", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH, CATCH_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.for", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR, FOR_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.if", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF, IF_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.switch", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH, SWITCH_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.synchronized", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED, SYNCHRONIZED_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.while", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE, WHILE_PREVIEW); //$NON-NLS-1$
        
        final InnerNode decls= createChild(parent, workingValues, "WhiteSpaceOptions.member_function_declaration"); { //$NON-NLS-1$
            createOption(decls, workingValues, "WhiteSpaceOptions.constructor", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
            createOption(decls, workingValues, "WhiteSpaceOptions.method", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        }
        createOption(parent, workingValues, "WhiteSpaceOptions.type_cast", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST, CAST_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.method_call", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.paren_expr", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION, PAREN_EXPR_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createBetweenEmptyParenTree(Map workingValues, final InnerNode parent) {
        
        createOption(parent, workingValues, "WhiteSpaceOptions.constructor_decl", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.method_decl", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.method_call", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION, METHOD_CALL_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createBetweenEmptyBracketsTree(Map workingValues, final InnerNode parent) {
        createOption(parent, workingValues, "WhiteSpaceOptions.array_alloc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACKETS_IN_ARRAY_ALLOCATION_EXPRESSION, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(parent, workingValues, "WhiteSpaceOptions.array_decl", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
    }
    
    private static void createBetweenEmptyBracesTree(Map workingValues, final InnerNode parent) {
        createOption(parent, workingValues, "WhiteSpaceOptions.initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
    }
    
    // syntax element tree

    private static InnerNode createClassTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.classes"); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.classes.before_opening_brace_of_a_class", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION, CLASS_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.classes.before_opening_brace_of_anon_class", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION, ANON_CLASS_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.classes.before_comma_implements", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES, CLASS_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.classes.after_comma_implements", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES, CLASS_DECL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createAssignmentTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.assignments"); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.assignments.before_assignment_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.assignments.after_assignment_operator", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createOperatorTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.operators"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.operators.before_binary_operators", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.operators.after_binary_operators", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.operators.before_unary_operators", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.operators.after_unary_operators", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.operators.before_prefix_operators", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.operators.after_prefix_operators", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.operators.before_postfix_operators", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.operators.after_postfix_operators", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR, OPERATOR_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createMethodDeclTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.methods"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.between_empty_parens", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_brace", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_comma_in_params", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_comma_in_params", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_comma_in_throws", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_comma_in_throws", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS, METHOD_DECL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createConstructorTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.constructors"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.between_empty_parens", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_brace", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_comma_in_params", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_comma_in_params", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_comma_in_throws", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_comma_in_throws", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createFieldTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.fields"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.fields.before_comma", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, MULT_FIELD_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.fields.after_comma", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, MULT_LOCAL_PREVIEW); //$NON-NLS-1$
        return root;
    }	
    
    private static InnerNode createLocalVariableTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.localvars"); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.localvars.before_comma", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, MULT_LOCAL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.localvars.after_comma", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, MULT_LOCAL_PREVIEW); //$NON-NLS-1$
        return root;
    }
   
    private static InnerNode createArrayInitializerTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.arrayinit"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_brace", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_brace", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_comma", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_comma", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.between_empty_braces", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createArrayDeclarationTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.arraydecls"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_bracket", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.between_empty_brackets", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createArrayElementAccessTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.arrayelem"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_bracket", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE, ARRAY_REF_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_bracket", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE, ARRAY_REF_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_bracket", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE, ARRAY_REF_PREVIEW); //$NON-NLS-1$
        
        return root;
    }
    
    private static InnerNode createArrayAllocTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.arrayalloc"); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_bracket", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_bracket", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_bracket", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.between_empty_brackets", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACKETS_IN_ARRAY_ALLOCATION_EXPRESSION, ARRAY_DECL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createFunctionCallTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.calls"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.between_empty_parens", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.calls.before_comma_in_method_args", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.calls.after_comma_in_method_args", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, METHOD_CALL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.calls.before_comma_in_alloc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION, ALLOC_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.calls.after_comma_in_alloc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION, ALLOC_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.calls.before_comma_in_qalloc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.calls.after_comma_in_qalloc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, CONSTRUCTOR_DECL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createBlockTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.blocks"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_brace", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK, BLOCK_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_closing_brace", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK, BLOCK_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createSwitchStatementTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.switch"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.switch.before_case_colon", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE, SWITCH_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.switch.before_default_colon", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT, SWITCH_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_brace", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH, SWITCH_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH, SWITCH_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH, SWITCH_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH, SWITCH_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createDoWhileTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.do"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE, WHILE_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE, WHILE_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE, WHILE_PREVIEW); //$NON-NLS-1$
        
        return root;
    }
    
    private static InnerNode createSynchronizedTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.synchronized"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED, SYNCHRONIZED_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED, SYNCHRONIZED_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED, SYNCHRONIZED_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createTryStatementTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.try"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH, CATCH_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH, CATCH_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH, CATCH_PREVIEW); //$NON-NLS-1$
        return root;
    }
    private static InnerNode createIfStatementTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.if"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF, IF_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF, IF_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF, IF_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    private static InnerNode createForStatementTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.for"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR, FOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR, FOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR, FOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.for.before_comma_init", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS, FOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.for.after_comma_init", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS, FOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.for.before_comma_inc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS, FOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.for.after_comma_inc", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS, FOR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_semicolon", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR, FOR_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
//  TODO: include this category
//  private static InnerNode createAssertTree(Map workingValues, InnerNode parent) {
//  "'assert'",
//  
//  new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT, "WhiteSpaceTabPage.before_colon"),
//  new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT, "WhiteSpaceTabPage.after_colon")
//  }
    
    
    private static InnerNode createLabelTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.labels"); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_colon", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT, LABEL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_colon", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT, LABEL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    
    private static InnerNode createConditionalTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.conditionals"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_question", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL, CONDITIONAL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_question", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL, CONDITIONAL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_colon", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL, CONDITIONAL_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_colon", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL, CONDITIONAL_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    
    private static InnerNode createTypecastTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.typecasts"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST, CAST_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST, CAST_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, CAST_PREVIEW); //$NON-NLS-1$
        return root;
    }
    
    
    private static InnerNode createParenthesizedExpressionTree(Map workingValues, InnerNode parent) {
        final InnerNode root= new InnerNode(parent, workingValues, "WhiteSpaceTabPage.parenexpr"); //$NON-NLS-1$
        
        createOption(root, workingValues, "WhiteSpaceTabPage.before_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION, PAREN_EXPR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.after_opening_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION, PAREN_EXPR_PREVIEW); //$NON-NLS-1$
        createOption(root, workingValues, "WhiteSpaceTabPage.before_closing_paren", DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION, PAREN_EXPR_PREVIEW); //$NON-NLS-1$
        return root;
	}
    
    
    
    private static InnerNode createChild(InnerNode root, Map workingValues, String messageKey) {
	    return new InnerNode(root, workingValues, messageKey);
	}
	
	private static OptionNode createOption(InnerNode root, Map workingValues, String messageKey, String key, PreviewSnippet snippet) {
	    return new OptionNode(root,workingValues, messageKey, key, snippet);
	}

	public static void makeIndexForNodes(List tree, List flatList) {
        for (final Iterator iter= tree.iterator(); iter.hasNext();) {
            final Node node= (Node) iter.next();
            node.index= flatList.size();
            flatList.add(node);
            makeIndexForNodes(node.getChildren(), flatList);
        }
    }
}
