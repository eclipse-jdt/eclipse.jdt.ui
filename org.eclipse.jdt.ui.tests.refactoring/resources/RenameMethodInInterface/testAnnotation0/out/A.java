/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package p;

/**
 * @see #ident
 * @see #ident()
 * @see I#ident()
 * @see p.I#name()
 */
@I(ident="X")
@interface I {
    @I()
    String ident() default IDefault.NAME;
    
    @I
    interface IDefault {
        public @I(ident=IDefault.NAME) final String NAME= "Me";
    }
}
