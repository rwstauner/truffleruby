/******************************************************************************/
/* This file is generated by the templates/template.rb script and should not  */
/* be modified manually. See                                                  */
/* templates/java/org/prism/Loader.java.erb                                   */
/* if you are looking to modify the                                           */
/* template                                                                   */
/******************************************************************************/

package org.prism;

import org.prism.Nodes;

import java.lang.Short;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

// GENERATED BY Loader.java.erb
// @formatter:off
public class Loader {

    public static ParseResult load(byte[] serialized, byte[] sourceBytes) {
        return new Loader(serialized, sourceBytes).load();
    }

    // Overridable methods

    public Charset getEncodingCharset(String encodingName) {
        encodingName = encodingName.toLowerCase(Locale.ROOT);
        if (encodingName.equals("ascii-8bit")) {
            return StandardCharsets.US_ASCII;
        }
        return Charset.forName(encodingName);
    }

    public String bytesToName(byte[] bytes) {
        return new String(bytes, encodingCharset).intern();
    }

    private static final class ConstantPool {

        private final Loader loader;
        private final byte[] source;
        private final int bufferOffset;
        private final String[] cache;

        ConstantPool(Loader loader, byte[] source, int bufferOffset, int length) {
            this.loader = loader;
            this.source = source;
            this.bufferOffset = bufferOffset;
            cache = new String[length];
        }

        String get(ByteBuffer buffer, int oneBasedIndex) {
            int index = oneBasedIndex - 1;
            String constant = cache[index];

            if (constant == null) {
                int offset = bufferOffset + index * 8;
                int start = buffer.getInt(offset);
                int length = buffer.getInt(offset + 4);

                byte[] bytes = new byte[length];

                if (Integer.compareUnsigned(start, 0x7FFFFFFF) <= 0) {
                    System.arraycopy(source, start, bytes, 0, length);
                } else {
                    int position = buffer.position();
                    buffer.position(start & 0x7FFFFFFF);
                    buffer.get(bytes, 0, length);
                    buffer.position(position);
                }

                constant = loader.bytesToName(bytes);
                cache[index] = constant;
            }

            return constant;
        }

    }

    private final ByteBuffer buffer;
    private final Nodes.Source source;
    protected String encodingName;
    private Charset encodingCharset;
    private ConstantPool constantPool;

    protected Loader(byte[] serialized, byte[] sourceBytes) {
        this.buffer = ByteBuffer.wrap(serialized).order(ByteOrder.nativeOrder());
        this.source = new Nodes.Source(sourceBytes);
    }

    protected ParseResult load() {
        expect((byte) 'P', "incorrect prism header");
        expect((byte) 'R', "incorrect prism header");
        expect((byte) 'I', "incorrect prism header");
        expect((byte) 'S', "incorrect prism header");
        expect((byte) 'M', "incorrect prism header");

        expect((byte) 0, "prism major version does not match");
        expect((byte) 27, "prism minor version does not match");
        expect((byte) 0, "prism patch version does not match");

        expect((byte) 1, "Loader.java requires no location fields in the serialized output");

        // This loads the name of the encoding.
        int encodingLength = loadVarUInt();
        byte[] encodingNameBytes = new byte[encodingLength];
        buffer.get(encodingNameBytes);
        this.encodingName = new String(encodingNameBytes, StandardCharsets.US_ASCII);
        this.encodingCharset = getEncodingCharset(this.encodingName);

        source.setStartLine(loadVarSInt());
        source.setLineOffsets(loadLineOffsets());

        ParseResult.MagicComment[] magicComments = loadMagicComments();
        Nodes.Location dataLocation = loadOptionalLocation();
        ParseResult.Error[] errors = loadErrors();
        ParseResult.Warning[] warnings = loadWarnings();

        int constantPoolBufferOffset = buffer.getInt();
        int constantPoolLength = loadVarUInt();
        this.constantPool = new ConstantPool(this, source.bytes, constantPoolBufferOffset, constantPoolLength);

        Nodes.Node node = loadNode();

        int left = constantPoolBufferOffset - buffer.position();
        if (left != 0) {
            throw new Error("Expected to consume all bytes while deserializing but there were " + left + " bytes left");
        }

        boolean[] newlineMarked = new boolean[1 + source.getLineCount()];
        MarkNewlinesVisitor visitor = new MarkNewlinesVisitor(source, newlineMarked);
        node.accept(visitor);

        return new ParseResult(node, magicComments, dataLocation, errors, warnings, source);
    }

    private byte[] loadEmbeddedString() {
        int length = loadVarUInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    private byte[] loadString() {
        switch (buffer.get()) {
            case 1:
                int start = loadVarUInt();
                int length = loadVarUInt();
                byte[] bytes = new byte[length];
                System.arraycopy(source.bytes, start, bytes, 0, length);
                return bytes;
            case 2:
                return loadEmbeddedString();
            default:
                throw new Error("Expected 0 or 1 but was " + buffer.get());
        }
    }

    private int[] loadLineOffsets() {
        int count = loadVarUInt();
        int[] lineOffsets = new int[count];
        for (int i = 0; i < count; i++) {
            lineOffsets[i] = loadVarUInt();
        }
        return lineOffsets;
    }

    private ParseResult.MagicComment[] loadMagicComments() {
        int count = loadVarUInt();
        ParseResult.MagicComment[] magicComments = new ParseResult.MagicComment[count];

        for (int i = 0; i < count; i++) {
            Nodes.Location keyLocation = loadLocation();
            Nodes.Location valueLocation = loadLocation();

            ParseResult.MagicComment magicComment = new ParseResult.MagicComment(keyLocation, valueLocation);
            magicComments[i] = magicComment;
        }

        return magicComments;
    }

    private ParseResult.Error[] loadErrors() {
        int count = loadVarUInt();
        ParseResult.Error[] errors = new ParseResult.Error[count];

        // error messages only contain ASCII characters
        for (int i = 0; i < count; i++) {
            Nodes.ErrorType type = Nodes.ERROR_TYPES[loadVarUInt()];
            byte[] bytes = loadEmbeddedString();
            String message = new String(bytes, StandardCharsets.US_ASCII);
            Nodes.Location location = loadLocation();
            ParseResult.ErrorLevel level = ParseResult.ERROR_LEVELS[buffer.get()];

            ParseResult.Error error = new ParseResult.Error(type, message, location, level);
            errors[i] = error;
        }

        return errors;
    }

    private ParseResult.Warning[] loadWarnings() {
        int count = loadVarUInt();
        ParseResult.Warning[] warnings = new ParseResult.Warning[count];

        // warning messages only contain ASCII characters
        for (int i = 0; i < count; i++) {
            Nodes.WarningType type = Nodes.WARNING_TYPES[loadVarUInt() - 257];
            byte[] bytes = loadEmbeddedString();
            String message = new String(bytes, StandardCharsets.US_ASCII);
            Nodes.Location location = loadLocation();
            ParseResult.WarningLevel level = ParseResult.WARNING_LEVELS[buffer.get()];

            ParseResult.Warning warning = new ParseResult.Warning(type, message, location, level);
            warnings[i] = warning;
        }

        return warnings;
    }

    private Nodes.Node loadOptionalNode() {
        if (buffer.get(buffer.position()) != 0) {
            return loadNode();
        } else {
            buffer.position(buffer.position() + 1); // continue after the 0 byte
            return null;
        }
    }

    private String loadConstant() {
        return constantPool.get(buffer, loadVarUInt());
    }

    private String loadOptionalConstant() {
        if (buffer.get(buffer.position()) != 0) {
            return loadConstant();
        } else {
            buffer.position(buffer.position() + 1); // continue after the 0 byte
            return null;
        }
    }

    private String[] loadConstants() {
        int length = loadVarUInt();
        if (length == 0) {
            return Nodes.EMPTY_STRING_ARRAY;
        }
        String[] constants = new String[length];
        for (int i = 0; i < length; i++) {
            constants[i] = constantPool.get(buffer, loadVarUInt());
        }
        return constants;
    }

    private Nodes.Location loadLocation() {
        return new Nodes.Location(loadVarUInt(), loadVarUInt());
    }

    private Nodes.Location loadOptionalLocation() {
        if (buffer.get() != 0) {
            return loadLocation();
        } else {
            return null;
        }
    }

    // From https://github.com/protocolbuffers/protobuf/blob/v23.1/java/core/src/main/java/com/google/protobuf/BinaryReader.java#L1507
    private int loadVarUInt() {
        int x;
        if ((x = buffer.get()) >= 0) {
            return x;
        } else if ((x ^= (buffer.get() << 7)) < 0) {
            x ^= (~0 << 7);
        } else if ((x ^= (buffer.get() << 14)) >= 0) {
            x ^= (~0 << 7) ^ (~0 << 14);
        } else if ((x ^= (buffer.get() << 21)) < 0) {
            x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
        } else {
            x ^= buffer.get() << 28;
            x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
        }
        return x;
    }

    // From https://github.com/protocolbuffers/protobuf/blob/v25.1/java/core/src/main/java/com/google/protobuf/CodedInputStream.java#L508-L510
    private int loadVarSInt() {
        int x = loadVarUInt();
        return (x >>> 1) ^ (-(x & 1));
    }

    private short loadFlags() {
        int flags = loadVarUInt();
        assert flags >= 0 && flags <= Short.MAX_VALUE;
        return (short) flags;
    }

    private static final BigInteger UNSIGNED_LONG_MASK = BigInteger.ONE.shiftLeft(Long.SIZE).subtract(BigInteger.ONE);

    private Object loadInteger() {
        boolean negative = buffer.get() != 0;

        int wordsLength = loadVarUInt();
        assert wordsLength > 0;

        // Load the first word. If it's the only word, then return an int if it
        // fits into one and a long otherwise.
        int firstWord = loadVarUInt();
        if (wordsLength == 1) {
            if (firstWord < 0) {
                if (negative && firstWord == Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }

                long words = Integer.toUnsignedLong(firstWord);
                return negative ? -words : words;
            }
            return negative ? -firstWord : firstWord;
        }

        // Load the second word. If there are only two words, then return a long
        // if it fits into one and a BigInteger otherwise.
        int secondWord = loadVarUInt();
        if (wordsLength == 2) {
            long words = (((long) secondWord) << 32L) | Integer.toUnsignedLong(firstWord);
            if (words < 0L) {
                if (negative && words == Long.MIN_VALUE) {
                    return Long.MIN_VALUE;
                }

                BigInteger result = BigInteger.valueOf(words).and(UNSIGNED_LONG_MASK);
                return negative ? result.negate() : result;
            }
            return negative ? -words : words;
        }

        // Otherwise, load the remaining words and return a BigInt.
        BigInteger result = BigInteger.valueOf(Integer.toUnsignedLong(firstWord));
        result = result.or(BigInteger.valueOf(Integer.toUnsignedLong(secondWord)).shiftLeft(32));

        for (int wordsIndex = 2; wordsIndex < wordsLength; wordsIndex++) {
            result = result.or(BigInteger.valueOf(Integer.toUnsignedLong(loadVarUInt())).shiftLeft(wordsIndex * 32));
        }

        return negative ? result.negate() : result;
    }

    private Nodes.Node loadNode() {
        int type = buffer.get() & 0xFF;
        int startOffset = loadVarUInt();
        int length = loadVarUInt();

        switch (type) {
            case 1:
                return new Nodes.AliasGlobalVariableNode(loadNode(), loadNode(), startOffset, length);
            case 2:
                return new Nodes.AliasMethodNode(loadNode(), loadNode(), startOffset, length);
            case 3:
                return new Nodes.AlternationPatternNode(loadNode(), loadNode(), startOffset, length);
            case 4:
                return new Nodes.AndNode(loadNode(), loadNode(), startOffset, length);
            case 5:
                return new Nodes.ArgumentsNode(loadFlags(), loadNodes(), startOffset, length);
            case 6:
                return new Nodes.ArrayNode(loadFlags(), loadNodes(), startOffset, length);
            case 7:
                return new Nodes.ArrayPatternNode(loadOptionalNode(), loadNodes(), loadOptionalNode(), loadNodes(), startOffset, length);
            case 8:
                return new Nodes.AssocNode(loadNode(), loadNode(), startOffset, length);
            case 9:
                return new Nodes.AssocSplatNode(loadOptionalNode(), startOffset, length);
            case 10:
                return new Nodes.BackReferenceReadNode(loadConstant(), startOffset, length);
            case 11:
                return new Nodes.BeginNode((Nodes.StatementsNode) loadOptionalNode(), (Nodes.RescueNode) loadOptionalNode(), (Nodes.ElseNode) loadOptionalNode(), (Nodes.EnsureNode) loadOptionalNode(), startOffset, length);
            case 12:
                return new Nodes.BlockArgumentNode(loadOptionalNode(), startOffset, length);
            case 13:
                return new Nodes.BlockLocalVariableNode(loadFlags(), loadConstant(), startOffset, length);
            case 14:
                return new Nodes.BlockNode(loadConstants(), loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 15:
                return new Nodes.BlockParameterNode(loadFlags(), loadOptionalConstant(), startOffset, length);
            case 16:
                return new Nodes.BlockParametersNode((Nodes.ParametersNode) loadOptionalNode(), loadBlockLocalVariableNodes(), startOffset, length);
            case 17:
                return new Nodes.BreakNode((Nodes.ArgumentsNode) loadOptionalNode(), startOffset, length);
            case 18:
                return new Nodes.CallAndWriteNode(loadFlags(), loadOptionalNode(), loadConstant(), loadConstant(), loadNode(), startOffset, length);
            case 19:
                return new Nodes.CallNode(loadFlags(), loadOptionalNode(), loadConstant(), (Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 20:
                return new Nodes.CallOperatorWriteNode(loadFlags(), loadOptionalNode(), loadConstant(), loadConstant(), loadConstant(), loadNode(), startOffset, length);
            case 21:
                return new Nodes.CallOrWriteNode(loadFlags(), loadOptionalNode(), loadConstant(), loadConstant(), loadNode(), startOffset, length);
            case 22:
                return new Nodes.CallTargetNode(loadFlags(), loadNode(), loadConstant(), startOffset, length);
            case 23:
                return new Nodes.CapturePatternNode(loadNode(), loadNode(), startOffset, length);
            case 24:
                return new Nodes.CaseMatchNode(loadOptionalNode(), loadNodes(), (Nodes.ElseNode) loadOptionalNode(), startOffset, length);
            case 25:
                return new Nodes.CaseNode(loadOptionalNode(), loadNodes(), (Nodes.ElseNode) loadOptionalNode(), startOffset, length);
            case 26:
                return new Nodes.ClassNode(loadConstants(), loadNode(), loadOptionalNode(), loadOptionalNode(), loadConstant(), startOffset, length);
            case 27:
                return new Nodes.ClassVariableAndWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 28:
                return new Nodes.ClassVariableOperatorWriteNode(loadConstant(), loadNode(), loadConstant(), startOffset, length);
            case 29:
                return new Nodes.ClassVariableOrWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 30:
                return new Nodes.ClassVariableReadNode(loadConstant(), startOffset, length);
            case 31:
                return new Nodes.ClassVariableTargetNode(loadConstant(), startOffset, length);
            case 32:
                return new Nodes.ClassVariableWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 33:
                return new Nodes.ConstantAndWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 34:
                return new Nodes.ConstantOperatorWriteNode(loadConstant(), loadNode(), loadConstant(), startOffset, length);
            case 35:
                return new Nodes.ConstantOrWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 36:
                return new Nodes.ConstantPathAndWriteNode((Nodes.ConstantPathNode) loadNode(), loadNode(), startOffset, length);
            case 37:
                return new Nodes.ConstantPathNode(loadOptionalNode(), loadNode(), startOffset, length);
            case 38:
                return new Nodes.ConstantPathOperatorWriteNode((Nodes.ConstantPathNode) loadNode(), loadNode(), loadConstant(), startOffset, length);
            case 39:
                return new Nodes.ConstantPathOrWriteNode((Nodes.ConstantPathNode) loadNode(), loadNode(), startOffset, length);
            case 40:
                return new Nodes.ConstantPathTargetNode(loadOptionalNode(), loadNode(), startOffset, length);
            case 41:
                return new Nodes.ConstantPathWriteNode((Nodes.ConstantPathNode) loadNode(), loadNode(), startOffset, length);
            case 42:
                return new Nodes.ConstantReadNode(loadConstant(), startOffset, length);
            case 43:
                return new Nodes.ConstantTargetNode(loadConstant(), startOffset, length);
            case 44:
                return new Nodes.ConstantWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 45:
                return new Nodes.DefNode(buffer.getInt(), loadConstant(), loadOptionalNode(), (Nodes.ParametersNode) loadOptionalNode(), loadOptionalNode(), loadConstants(), startOffset, length);
            case 46:
                return new Nodes.DefinedNode(loadNode(), startOffset, length);
            case 47:
                return new Nodes.ElseNode((Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 48:
                return new Nodes.EmbeddedStatementsNode((Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 49:
                return new Nodes.EmbeddedVariableNode(loadNode(), startOffset, length);
            case 50:
                return new Nodes.EnsureNode((Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 51:
                return new Nodes.FalseNode(startOffset, length);
            case 52:
                return new Nodes.FindPatternNode(loadOptionalNode(), loadNode(), loadNodes(), loadNode(), startOffset, length);
            case 53:
                return new Nodes.FlipFlopNode(loadFlags(), loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 54:
                return new Nodes.FloatNode(buffer.getDouble(), startOffset, length);
            case 55:
                return new Nodes.ForNode(loadNode(), loadNode(), (Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 56:
                return new Nodes.ForwardingArgumentsNode(startOffset, length);
            case 57:
                return new Nodes.ForwardingParameterNode(startOffset, length);
            case 58:
                return new Nodes.ForwardingSuperNode((Nodes.BlockNode) loadOptionalNode(), startOffset, length);
            case 59:
                return new Nodes.GlobalVariableAndWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 60:
                return new Nodes.GlobalVariableOperatorWriteNode(loadConstant(), loadNode(), loadConstant(), startOffset, length);
            case 61:
                return new Nodes.GlobalVariableOrWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 62:
                return new Nodes.GlobalVariableReadNode(loadConstant(), startOffset, length);
            case 63:
                return new Nodes.GlobalVariableTargetNode(loadConstant(), startOffset, length);
            case 64:
                return new Nodes.GlobalVariableWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 65:
                return new Nodes.HashNode(loadNodes(), startOffset, length);
            case 66:
                return new Nodes.HashPatternNode(loadOptionalNode(), loadAssocNodes(), loadOptionalNode(), startOffset, length);
            case 67:
                return new Nodes.IfNode(loadNode(), (Nodes.StatementsNode) loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 68:
                return new Nodes.ImaginaryNode(loadNode(), startOffset, length);
            case 69:
                return new Nodes.ImplicitNode(loadNode(), startOffset, length);
            case 70:
                return new Nodes.ImplicitRestNode(startOffset, length);
            case 71:
                return new Nodes.InNode(loadNode(), (Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 72:
                return new Nodes.IndexAndWriteNode(loadFlags(), loadOptionalNode(), (Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalNode(), loadNode(), startOffset, length);
            case 73:
                return new Nodes.IndexOperatorWriteNode(loadFlags(), loadOptionalNode(), (Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalNode(), loadConstant(), loadNode(), startOffset, length);
            case 74:
                return new Nodes.IndexOrWriteNode(loadFlags(), loadOptionalNode(), (Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalNode(), loadNode(), startOffset, length);
            case 75:
                return new Nodes.IndexTargetNode(loadFlags(), loadNode(), (Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 76:
                return new Nodes.InstanceVariableAndWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 77:
                return new Nodes.InstanceVariableOperatorWriteNode(loadConstant(), loadNode(), loadConstant(), startOffset, length);
            case 78:
                return new Nodes.InstanceVariableOrWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 79:
                return new Nodes.InstanceVariableReadNode(loadConstant(), startOffset, length);
            case 80:
                return new Nodes.InstanceVariableTargetNode(loadConstant(), startOffset, length);
            case 81:
                return new Nodes.InstanceVariableWriteNode(loadConstant(), loadNode(), startOffset, length);
            case 82:
                return new Nodes.IntegerNode(loadFlags(), loadInteger(), startOffset, length);
            case 83:
                return new Nodes.InterpolatedMatchLastLineNode(loadFlags(), loadNodes(), startOffset, length);
            case 84:
                return new Nodes.InterpolatedRegularExpressionNode(loadFlags(), loadNodes(), startOffset, length);
            case 85:
                return new Nodes.InterpolatedStringNode(loadFlags(), loadNodes(), startOffset, length);
            case 86:
                return new Nodes.InterpolatedSymbolNode(loadNodes(), startOffset, length);
            case 87:
                return new Nodes.InterpolatedXStringNode(loadNodes(), startOffset, length);
            case 88:
                return new Nodes.ItParametersNode(startOffset, length);
            case 89:
                return new Nodes.KeywordHashNode(loadFlags(), loadNodes(), startOffset, length);
            case 90:
                return new Nodes.KeywordRestParameterNode(loadFlags(), loadOptionalConstant(), startOffset, length);
            case 91:
                return new Nodes.LambdaNode(loadConstants(), loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 92:
                return new Nodes.LocalVariableAndWriteNode(loadNode(), loadConstant(), loadVarUInt(), startOffset, length);
            case 93:
                return new Nodes.LocalVariableOperatorWriteNode(loadNode(), loadConstant(), loadConstant(), loadVarUInt(), startOffset, length);
            case 94:
                return new Nodes.LocalVariableOrWriteNode(loadNode(), loadConstant(), loadVarUInt(), startOffset, length);
            case 95:
                return new Nodes.LocalVariableReadNode(loadConstant(), loadVarUInt(), startOffset, length);
            case 96:
                return new Nodes.LocalVariableTargetNode(loadConstant(), loadVarUInt(), startOffset, length);
            case 97:
                return new Nodes.LocalVariableWriteNode(loadConstant(), loadVarUInt(), loadNode(), startOffset, length);
            case 98:
                return new Nodes.MatchLastLineNode(loadFlags(), loadString(), startOffset, length);
            case 99:
                return new Nodes.MatchPredicateNode(loadNode(), loadNode(), startOffset, length);
            case 100:
                return new Nodes.MatchRequiredNode(loadNode(), loadNode(), startOffset, length);
            case 101:
                return new Nodes.MatchWriteNode((Nodes.CallNode) loadNode(), loadLocalVariableTargetNodes(), startOffset, length);
            case 102:
                return new Nodes.MissingNode(startOffset, length);
            case 103:
                return new Nodes.ModuleNode(loadConstants(), loadNode(), loadOptionalNode(), loadConstant(), startOffset, length);
            case 104:
                return new Nodes.MultiTargetNode(loadNodes(), loadOptionalNode(), loadNodes(), startOffset, length);
            case 105:
                return new Nodes.MultiWriteNode(loadNodes(), loadOptionalNode(), loadNodes(), loadNode(), startOffset, length);
            case 106:
                return new Nodes.NextNode((Nodes.ArgumentsNode) loadOptionalNode(), startOffset, length);
            case 107:
                return new Nodes.NilNode(startOffset, length);
            case 108:
                return new Nodes.NoKeywordsParameterNode(startOffset, length);
            case 109:
                return new Nodes.NumberedParametersNode(buffer.get(), startOffset, length);
            case 110:
                return new Nodes.NumberedReferenceReadNode(loadVarUInt(), startOffset, length);
            case 111:
                return new Nodes.OptionalKeywordParameterNode(loadFlags(), loadConstant(), loadNode(), startOffset, length);
            case 112:
                return new Nodes.OptionalParameterNode(loadFlags(), loadConstant(), loadNode(), startOffset, length);
            case 113:
                return new Nodes.OrNode(loadNode(), loadNode(), startOffset, length);
            case 114:
                return new Nodes.ParametersNode(loadNodes(), loadOptionalParameterNodes(), loadOptionalNode(), loadNodes(), loadNodes(), loadOptionalNode(), (Nodes.BlockParameterNode) loadOptionalNode(), startOffset, length);
            case 115:
                return new Nodes.ParenthesesNode(loadOptionalNode(), startOffset, length);
            case 116:
                return new Nodes.PinnedExpressionNode(loadNode(), startOffset, length);
            case 117:
                return new Nodes.PinnedVariableNode(loadNode(), startOffset, length);
            case 118:
                return new Nodes.PostExecutionNode((Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 119:
                return new Nodes.PreExecutionNode((Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 120:
                return new Nodes.ProgramNode(loadConstants(), (Nodes.StatementsNode) loadNode(), startOffset, length);
            case 121:
                return new Nodes.RangeNode(loadFlags(), loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 122:
                return new Nodes.RationalNode(loadNode(), startOffset, length);
            case 123:
                return new Nodes.RedoNode(startOffset, length);
            case 124:
                return new Nodes.RegularExpressionNode(loadFlags(), loadString(), startOffset, length);
            case 125:
                return new Nodes.RequiredKeywordParameterNode(loadFlags(), loadConstant(), startOffset, length);
            case 126:
                return new Nodes.RequiredParameterNode(loadFlags(), loadConstant(), startOffset, length);
            case 127:
                return new Nodes.RescueModifierNode(loadNode(), loadNode(), startOffset, length);
            case 128:
                return new Nodes.RescueNode(loadNodes(), loadOptionalNode(), (Nodes.StatementsNode) loadOptionalNode(), (Nodes.RescueNode) loadOptionalNode(), startOffset, length);
            case 129:
                return new Nodes.RestParameterNode(loadFlags(), loadOptionalConstant(), startOffset, length);
            case 130:
                return new Nodes.RetryNode(startOffset, length);
            case 131:
                return new Nodes.ReturnNode((Nodes.ArgumentsNode) loadOptionalNode(), startOffset, length);
            case 132:
                return new Nodes.SelfNode(startOffset, length);
            case 133:
                return new Nodes.ShareableConstantNode(loadFlags(), loadNode(), startOffset, length);
            case 134:
                return new Nodes.SingletonClassNode(loadConstants(), loadNode(), loadOptionalNode(), startOffset, length);
            case 135:
                return new Nodes.SourceEncodingNode(startOffset, length);
            case 136:
                return new Nodes.SourceFileNode(loadFlags(), loadString(), startOffset, length);
            case 137:
                return new Nodes.SourceLineNode(startOffset, length);
            case 138:
                return new Nodes.SplatNode(loadOptionalNode(), startOffset, length);
            case 139:
                return new Nodes.StatementsNode(loadNodes(), startOffset, length);
            case 140:
                return new Nodes.StringNode(loadFlags(), loadString(), startOffset, length);
            case 141:
                return new Nodes.SuperNode((Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 142:
                return new Nodes.SymbolNode(loadFlags(), loadString(), startOffset, length);
            case 143:
                return new Nodes.TrueNode(startOffset, length);
            case 144:
                return new Nodes.UndefNode(loadNodes(), startOffset, length);
            case 145:
                return new Nodes.UnlessNode(loadNode(), (Nodes.StatementsNode) loadOptionalNode(), (Nodes.ElseNode) loadOptionalNode(), startOffset, length);
            case 146:
                return new Nodes.UntilNode(loadFlags(), loadNode(), (Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 147:
                return new Nodes.WhenNode(loadNodes(), (Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 148:
                return new Nodes.WhileNode(loadFlags(), loadNode(), (Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 149:
                return new Nodes.XStringNode(loadFlags(), loadString(), startOffset, length);
            case 150:
                return new Nodes.YieldNode((Nodes.ArgumentsNode) loadOptionalNode(), startOffset, length);
            default:
                throw new Error("Unknown node type: " + type);
        }
    }

    private static final Nodes.Node[] EMPTY_Node_ARRAY = {};

    private Nodes.Node[] loadNodes() {
        int length = loadVarUInt();
        if (length == 0) {
            return EMPTY_Node_ARRAY;
        }
        Nodes.Node[] nodes = new Nodes.Node[length];
        for (int i = 0; i < length; i++) {
            nodes[i] = loadNode();
        }
        return nodes;
    }

    private static final Nodes.BlockLocalVariableNode[] EMPTY_BlockLocalVariableNode_ARRAY = {};

    private Nodes.BlockLocalVariableNode[] loadBlockLocalVariableNodes() {
        int length = loadVarUInt();
        if (length == 0) {
            return EMPTY_BlockLocalVariableNode_ARRAY;
        }
        Nodes.BlockLocalVariableNode[] nodes = new Nodes.BlockLocalVariableNode[length];
        for (int i = 0; i < length; i++) {
            nodes[i] = (Nodes.BlockLocalVariableNode) loadNode();
        }
        return nodes;
    }

    private static final Nodes.AssocNode[] EMPTY_AssocNode_ARRAY = {};

    private Nodes.AssocNode[] loadAssocNodes() {
        int length = loadVarUInt();
        if (length == 0) {
            return EMPTY_AssocNode_ARRAY;
        }
        Nodes.AssocNode[] nodes = new Nodes.AssocNode[length];
        for (int i = 0; i < length; i++) {
            nodes[i] = (Nodes.AssocNode) loadNode();
        }
        return nodes;
    }

    private static final Nodes.LocalVariableTargetNode[] EMPTY_LocalVariableTargetNode_ARRAY = {};

    private Nodes.LocalVariableTargetNode[] loadLocalVariableTargetNodes() {
        int length = loadVarUInt();
        if (length == 0) {
            return EMPTY_LocalVariableTargetNode_ARRAY;
        }
        Nodes.LocalVariableTargetNode[] nodes = new Nodes.LocalVariableTargetNode[length];
        for (int i = 0; i < length; i++) {
            nodes[i] = (Nodes.LocalVariableTargetNode) loadNode();
        }
        return nodes;
    }

    private static final Nodes.OptionalParameterNode[] EMPTY_OptionalParameterNode_ARRAY = {};

    private Nodes.OptionalParameterNode[] loadOptionalParameterNodes() {
        int length = loadVarUInt();
        if (length == 0) {
            return EMPTY_OptionalParameterNode_ARRAY;
        }
        Nodes.OptionalParameterNode[] nodes = new Nodes.OptionalParameterNode[length];
        for (int i = 0; i < length; i++) {
            nodes[i] = (Nodes.OptionalParameterNode) loadNode();
        }
        return nodes;
    }

    private void expect(byte value, String error) {
        byte b = buffer.get();
        if (b != value) {
            throw new Error("Deserialization error: " + error + " (expected " + value + " but was " + b + " at position " + buffer.position() + ")");
        }
    }

}
// @formatter:on
