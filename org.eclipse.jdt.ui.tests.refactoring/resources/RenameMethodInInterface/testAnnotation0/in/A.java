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
 * @see #name
 * @see #name()
 * @see I#name()
 * @see p.I#name()
 */
@I(name="X")
@interface I {
    @I()
    String name() default IDefault.NAME;
    
    @I
    interface IDefault {
        public @I(name=IDefault.NAME) final String NAME= "Me";
    }
}
