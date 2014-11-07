/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.*;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.objectstorage.*;

/**
 * Represents the Ruby {@code Class} class. Note that most of the functionality you might associate
 * with {@code Class} is actually in {@code Module}, implemented by {@link RubyModule}.
 */
public class RubyClass extends RubyModule {

    private boolean isSingleton;
    private final Set<RubyClass> subClasses = Collections.newSetFromMap(new WeakHashMap<RubyClass, Boolean>());
    private ObjectLayout objectLayoutForInstances = null;

    /**
     * The class from which we create the object that is {@code Class}. A subclass of
     * {@link RubyClass} so that we can override {@link #newInstance} and allocate a
     * {@link RubyClass} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyClassClass extends RubyClass {

        public RubyClassClass(RubyContext context) {
            super(null, context, null, null, null, "Class", false);
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyClass(null, null, getContext().getCoreLibrary().getObjectClass(), "(unnamed class)");
        }

    }

    /**
     * This constructor supports initialization and solves boot-order problems and should not
     * normally be used from outside this class.
     */
    public static RubyClass createBootClass(RubyContext context, RubyClass classClass,String name) {
        return new RubyClass(null, context, classClass, null, null, name, false);
    }

    public static RubyClass createSingletonClass(Node currentNode, RubyContext context, RubyModule lexicalParent, RubyClass superclass, String name) {
        return new RubyClass(currentNode, context, context.getCoreLibrary().getClassClass(), lexicalParent, superclass, name, true);
    }

    public RubyClass(Node currentNode, RubyModule lexicalParent, RubyClass superclass, String name) {
        this(currentNode, superclass.getContext(), superclass.getContext().getCoreLibrary().getClassClass(), lexicalParent, superclass, name, false);

        // Always create a class singleton class for normal classes for consistency.
        createSingletonClass(currentNode);
    }

    protected RubyClass(Node currentNode, RubyContext context, RubyClass classClass, RubyModule lexicalParent, RubyClass superclass, String name, boolean isSingleton) {
        super(context, classClass, lexicalParent, name);
        this.isSingleton = isSingleton;

        if (superclass == null) {
            objectLayoutForInstances = ObjectLayout.EMPTY;
        } else {
            unsafeSetSuperclass(superclass);
        }
    }

    @Override
    public void initCopy(RubyModule other) {
        super.initCopy(other);
        assert other instanceof RubyClass;
        final RubyClass otherClass = (RubyClass) other;
        this.objectLayoutForInstances = otherClass.objectLayoutForInstances;
    }

    @Override
    public RubyClass getSingletonClass(Node currentNode) {
        RubyClass singletonClass = createSingletonClass(currentNode);

        // We also need to create the singleton class of singletonClass for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        singletonClass.createSingletonClass(currentNode);

        return singletonClass;
    }

    private RubyClass createSingletonClass(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        if (metaClass.isSingleton()) {
            return metaClass;
        }

        RubyClass singletonSuperclass;

        if (getSuperClass() == null) {
            singletonSuperclass = getLogicalClass();
        } else {
            singletonSuperclass = getSuperClass().createSingletonClass(currentNode);
        }

        metaClass = RubyClass.createSingletonClass(currentNode, getContext(), null,
                singletonSuperclass, String.format("#<Class:%s>", getName()));

        return metaClass;
    }

    /**
     * This method supports initialization and solves boot-order problems and should not normally be
     * used.
     */
    public void unsafeSetSuperclass(RubyClass newSuperclass) {
        RubyNode.notDesignedForCompilation();

        assert parentModule == null;

        unsafeSetParent(newSuperclass);
        newSuperclass.subClasses.add(this);

        objectLayoutForInstances = new ObjectLayout(newSuperclass.objectLayoutForInstances);
    }

    @SlowPath
    public RubyBasicObject newInstance(RubyNode currentNode) {
        return new RubyObject(this);
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    /**
     * Returns the object layout that objects of this class should use. Do not confuse with
     * {@link #getObjectLayout}, which for {@link RubyClass} will return the layout of the class
     * object itself.
     */
    public ObjectLayout getObjectLayoutForInstances() {
        return objectLayoutForInstances;
    }

    /**
     * Change the layout to be used for instances of this object.
     */
    public void setObjectLayoutForInstances(ObjectLayout newObjectLayoutForInstances) {
        RubyNode.notDesignedForCompilation();

        assert newObjectLayoutForInstances != objectLayoutForInstances;

        objectLayoutForInstances = newObjectLayoutForInstances;

        for (RubyClass subClass : subClasses) {
            subClass.renewObjectLayoutForInstances();
        }
    }

    private void renewObjectLayoutForInstances() {
        RubyNode.notDesignedForCompilation();

        objectLayoutForInstances = objectLayoutForInstances.withNewParent(getSuperClass().objectLayoutForInstances);

        for (RubyClass subClass : subClasses) {
            subClass.renewObjectLayoutForInstances();
        }
    }

    public RubyClass getSuperClass() {
        CompilerAsserts.neverPartOfCompilation();

        for (RubyModule ancestor : parentAncestors()) {
            if (ancestor instanceof RubyClass) {
                return (RubyClass) ancestor;
            }
        }

        return null;
    }

}
