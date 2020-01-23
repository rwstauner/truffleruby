/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language.arguments;

import org.truffleruby.language.ContextSourceRubyNode;
import org.truffleruby.language.NotProvided;

import com.oracle.truffle.api.frame.VirtualFrame;

public class NotProvidedNode extends ContextSourceRubyNode {

    @Override
    public Object execute(VirtualFrame frame) {
        return NotProvided.INSTANCE;
    }

}
