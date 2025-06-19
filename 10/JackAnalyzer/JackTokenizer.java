package JackAnalyzer;

// JackTokenizer.java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jack词法分析器 (Tokenizer)。
 * 负责读取一个.jack源文件，将其分解为一系列的Jack词法单元（tokens）。
 * 这个类会处理并移除所有的注释和空白符。
 */
public class JackTokenizer {

    /**
     * 定义Jack语言中所有可能的词法单元类型。
     */
    public enum TokenType {
        /** 关键字，例如 'class', 'let', 'if'等 */
        KEYWORD,
        /** 符号，例如 '{', '(', ';', '+'等 */
        SYMBOL,
        /** 标识符，即变量名、类名、子程序名等 */
        IDENTIFIER,
        /** 整数常量 */
        INT_CONST,
        /** 字符串常量（不包含双引号） */
        STRING_CONST
    }

    // 使用集合（Set）来快速查找关键字和符号
    private static final Set<String> KEYWORDS = new HashSet<>();
    private static final Set<Character> SYMBOLS = new HashSet<>();

    // 静态初始化块，用于填充关键字和符号集合
    static {
        KEYWORDS.add("class"); KEYWORDS.add("constructor"); KEYWORDS.add("function");
        KEYWORDS.add("method"); KEYWORDS.add("field"); KEYWORDS.add("static");
        KEYWORDS.add("var"); KEYWORDS.add("int"); KEYWORDS.add("char");
        KEYWORDS.add("boolean"); KEYWORDS.add("void"); KEYWORDS.add("true");
        KEYWORDS.add("false"); KEYWORDS.add("null"); KEYWORDS.add("this");
        KEYWORDS.add("let"); KEYWORDS.add("do"); KEYWORDS.add("if");
        KEYWORDS.add("else"); KEYWORDS.add("while"); KEYWORDS.add("return");

        SYMBOLS.add('{'); SYMBOLS.add('}'); SYMBOLS.add('('); SYMBOLS.add(')');
        SYMBOLS.add('['); SYMBOLS.add(']'); SYMBOLS.add('.'); SYMBOLS.add(',');
        SYMBOLS.add(';'); SYMBOLS.add('+'); SYMBOLS.add('-'); SYMBOLS.add('*');
        SYMBOLS.add('/'); SYMBOLS.add('&'); SYMBOLS.add('|'); SYMBOLS.add('<');
        SYMBOLS.add('>'); SYMBOLS.add('='); SYMBOLS.add('~');
    }

    private ArrayList<String> tokens;
    private int currentTokenIndex;
    private String currentToken;

    /**
     * 构造函数。打开输入文件，读取内容，移除注释，并将其分解为词法单元列表。
     * @param inputFile 输入的 .jack 文件路径。
     * @throws IOException 如果文件读取失败。
     */
    public JackTokenizer(String inputFile) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(inputFile)));

        // 1. 移除所有块注释 (/* ... */)
        content = content.replaceAll("/\\*.*?\\*/", " ");
        // 2. 移除所有行注释 (// ...)
        content = content.replaceAll("//.*", "");

        this.tokens = new ArrayList<>();
        // 3. 使用正则表达式一次性匹配所有类型的词法单元
        // 这个正则表达式会匹配:
        // - "[^"]*"       : 一个完整的字符串常量（包括双引号）
        // - \\w+           : 一个或多个单词字符（字母、数字、下划线），代表关键字或标识符
        // - [{}()\\[\\].,;+\\-*/&|<>=~] : 单个符号字符
        Pattern pattern = Pattern.compile("\"[^\"]*\"|\\w+|[{}()\\[\\].,;+\\-*/&|<>=~]");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            this.tokens.add(matcher.group());
        }

        this.currentTokenIndex = -1;
        this.currentToken = null;
    }

    /**
     * 检查输入中是否还有更多的词法单元。
     * @return 如果有更多词法单元则返回true，否则返回false。
     */
    public boolean hasMoreTokens() {
        return currentTokenIndex < tokens.size() - 1;
    }

    /**
     * 从输入中获取下一个词法单元，并使其成为“当前”词法单元。
     * 这个方法应该只在 hasMoreTokens() 为 true 时调用。
     */
    public void advance() {
        if (hasMoreTokens()) {
            currentTokenIndex++;
            currentToken = tokens.get(currentTokenIndex);
        } else {
            currentToken = null; // 表示没有更多词法单元
        }
    }

    /**
     * 返回当前词法单元的类型。
     * @return 当前词法单元的TokenType枚举值。
     */
    public TokenType tokenType() {
        if (KEYWORDS.contains(currentToken)) {
            return TokenType.KEYWORD;
        }
        if (currentToken.length() == 1 && SYMBOLS.contains(currentToken.charAt(0))) {
            return TokenType.SYMBOL;
        }
        // 使用正则表达式判断是否为纯数字
        if (currentToken.matches("\\d+")) {
            return TokenType.INT_CONST;
        }
        // 判断是否为字符串常量（以"开头和结尾）
        if (currentToken.startsWith("\"") && currentToken.endsWith("\"")) {
            return TokenType.STRING_CONST;
        }
        // 判断是否为标识符（以字母或下划线开头，后跟字母、数字或下划线）
        if (currentToken.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return TokenType.IDENTIFIER;
        }
        // 如果以上都不是，则抛出异常
        throw new IllegalStateException("无法识别的词法单元: " + currentToken);
    }

    /**
     * @return 返回当前关键字。仅当 tokenType() 是 KEYWORD 时调用。
     */
    public String keyword() {
        return currentToken;
    }

    /**
     * @return 返回当前符号。仅当 tokenType() 是 SYMBOL 时调用。
     */
    public char symbol() {
        return currentToken.charAt(0);
    }

    /**
     * @return 返回当前标识符。仅当 tokenType() 是 IDENTIFIER 时调用。
     */
    public String identifier() {
        return currentToken;
    }

    /**
     * @return 返回当前整数常量的值。仅当 tokenType() 是 INT_CONST 时调用。
     */
    public int intVal() {
        return Integer.parseInt(currentToken);
    }

    /**
     * @return 返回当前字符串常量的值（不包含两边的双引号）。仅当 tokenType() 是 STRING_CONST 时调用。
     */
    public String stringVal() {
        return currentToken.substring(1, currentToken.length() - 1);
    }

    /**
     * （辅助方法）获取下一个词法单元用于向前看（lookahead），但不消耗它。
     * @return 下一个词法单元的字符串，如果没有则返回null。
     */
    public String peek() {
        if (hasMoreTokens()) {
            return tokens.get(currentTokenIndex + 1);
        }
        return null;
    }
}