package org.truffleruby.language;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.truffleruby.RubyContext;
import org.truffleruby.core.CoreLibrary;
import org.truffleruby.core.array.ArrayHelpers;
import org.truffleruby.core.exception.CoreExceptions;
import org.truffleruby.core.kernel.TraceManager;
import org.truffleruby.core.numeric.BignumOperations;
import org.truffleruby.core.rope.Rope;
import org.truffleruby.core.string.CoreStrings;
import org.truffleruby.stdlib.CoverageManager;

/**RubyNode has source, execute, and is instrument-able.
 * However, it does not have any fields which would prevent using @GenerateUncached.
 * It should never be subclassed directly, either use {@link ContextSourceRubyNode}
 * or {@link UncacheableSourceRubyNode}.
 * SourceRubyNode is not defined since there was no use for it for now.
 * Nodes having context are described by {@link WithContext}.
 * There is also {@link ContextRubyNode} if context is needed but source is not. */
public abstract class RubyNode extends BaseRubyNode implements InstrumentableNode {

    private static final byte FLAG_NEWLINE = 0;
    private static final byte FLAG_COVERAGE_LINE = 1;
    private static final byte FLAG_CALL = 2;
    private static final byte FLAG_ROOT = 3;

    protected static final int NO_SOURCE = -1;

    {
        setSourceCharIndex(NO_SOURCE);
    }

    abstract public Object isDefined(VirtualFrame frame);

    // Fundamental execute methods

    abstract public Object execute(VirtualFrame frame);

    /**
     * This method does not start with "execute" on purpose, so the Truffle DSL does not generate
     * useless copies of this method which would increase the number of runtime compilable methods.
     */
    public void doExecuteVoid(VirtualFrame frame) {
        execute(frame);
    }

    // Source

    abstract protected int getSourceCharIndex();

    abstract protected void setSourceCharIndex(int sourceCharIndex);

    abstract protected int getSourceLength();

    abstract protected void setSourceLength(int sourceLength);

    public boolean hasSource() {
        return getSourceCharIndex() != NO_SOURCE;
    }

    public void unsafeSetSourceSection(SourceIndexLength sourceIndexLength) {
        assert !hasSource();

        if (sourceIndexLength != null) {
            setSourceCharIndex(sourceIndexLength.getCharIndex());
            setSourceLength(sourceIndexLength.getLength());
        }
    }

    public void unsafeSetSourceSection(SourceSection sourceSection) {
        assert !hasSource();

        if (sourceSection.isAvailable()) {
            setSourceCharIndex(sourceSection.getCharIndex());
            setSourceLength(sourceSection.getCharLength());
        } else {
            setSourceCharIndex(0);
            setSourceLength(SourceIndexLength.UNAVAILABLE);
        }
    }

    public SourceIndexLength getSourceIndexLength() {
        if (!hasSource()) {
            return null;
        } else {
            return new SourceIndexLength(getSourceCharIndex(), getSourceLength());
        }
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public SourceSection getSourceSection() {
        if (!hasSource()) {
            return null;
        } else {
            final com.oracle.truffle.api.source.Source source = getSource();

            if (source == null) {
                return null;
            }

            return getSourceIndexLength().toSourceSection(source);
        }

    }

    private com.oracle.truffle.api.source.Source getSource() {
        final RootNode rootNode = getRootNode();

        if (rootNode == null) {
            return null;
        }

        final SourceSection sourceSection = rootNode.getSourceSection();

        if (sourceSection == null) {
            return null;
        }

        return sourceSection.getSource();
    }

    public SourceIndexLength getEncapsulatingSourceIndexLength() {
        Node node = this;
        while (node != null) {
            if (node instanceof RubyNode && ((RubyNode) node).hasSource()) {
                return ((RubyNode) node).getSourceIndexLength();
            }

            if (node instanceof RootNode) {
                return new SourceIndexLength(node.getSourceSection());
            }

            node = node.getParent();
        }

        return null;
    }

    // Instrumentation

    @Override
    public boolean isInstrumentable() {
        return hasSource();
    }

    protected abstract byte getFlags();

    protected abstract void setFlags(byte flags);

    private void setFlag(byte flag) {
        setFlags((byte) (getFlags() | 1 << flag));
    }

    public void unsafeSetIsNewLine() {
        setFlag(FLAG_NEWLINE);
    }

    public void unsafeSetIsCoverageLine() {
        setFlag(FLAG_COVERAGE_LINE);
    }

    public void unsafeSetIsCall() {
        setFlag(FLAG_CALL);
    }

    public void unsafeSetIsRoot() {
        setFlag(FLAG_ROOT);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        byte flags = getFlags();
        if (tag == TraceManager.CallTag.class || tag == StandardTags.CallTag.class) {
            return isTag(flags, FLAG_CALL);
        }

        if (tag == TraceManager.LineTag.class || tag == StandardTags.StatementTag.class) {
            return isTag(flags, FLAG_NEWLINE);
        }

        if (tag == CoverageManager.LineTag.class) {
            return isTag(flags, FLAG_COVERAGE_LINE);
        }

        if (tag == StandardTags.RootTag.class) {
            return isTag(flags, FLAG_ROOT);
        }

        return false;
    }

    private static boolean isTag(byte flags, byte flag) {
        return ((flags >> flag) & 1) == 1;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new RubyNodeWrapperWithIsDefined(this, probe);
    }

    public interface WithContext {
        RubyContext getContext();

        // Guards which use the context and so can't be static

        default boolean isNil(Object value) {
            return value == nil();
        }

        // Helpers methods for terseness, keep in sync

        default DynamicObject nil() {
            return coreLibrary().nil;
        }

        default DynamicObject getSymbol(String name) {
            return getContext().getSymbolTable().getSymbol(name);
        }

        default DynamicObject getSymbol(Rope name) {
            return getContext().getSymbolTable().getSymbol(name);
        }

        default Encoding getLocaleEncoding() {
            return getContext().getEncodingManager().getLocaleEncoding();
        }

        default DynamicObject createArray(Object store, int size) {
            return ArrayHelpers.createArray(getContext(), store, size);
        }

        default DynamicObject createArray(Object[] store) {
            return createArray(store, store.length);
        }

        default DynamicObject createBignum(BigInteger value) {
            return BignumOperations.createBignum(getContext(), value);
        }

        default CoreStrings coreStrings() {
            return getContext().getCoreStrings();
        }

        default CoreLibrary coreLibrary() {
            return getContext().getCoreLibrary();
        }

        default CoreExceptions coreExceptions() {
            return getContext().getCoreExceptions();
        }

        default int getIdentityCacheLimit() {
            return getContext().getOptions().IDENTITY_CACHE;
        }

        default int getDefaultCacheLimit() {
            return getContext().getOptions().DEFAULT_CACHE;
        }
    }
}
