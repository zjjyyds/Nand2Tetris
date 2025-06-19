package JackAnalyzer;

// CompilationEngine.java
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 编译引擎 (Compilation Engine)。
 * 这是语法分析的核心，它接收来自JackTokenizer的词法单元流，
 * 并根据Jack语言的语法规则，生成一个结构化的XML输出文件。
 * 它采用递归下降（Recursive Descent）的解析策略。
 */
public class CompilationEngine {

    private JackTokenizer tokenizer;
    private PrintWriter writer;
    private int indent = 0;
    // 用于转换XML特殊符号的映射表
    private static final Map<Character, String> SYMBOL_MAP = new HashMap<>();

    static {
        SYMBOL_MAP.put('<', "&lt;");
        SYMBOL_MAP.put('>', "&gt;");
        SYMBOL_MAP.put('"', "&quot;");
        SYMBOL_MAP.put('&', "&amp;");
    }

    /**
     * 创建一个新的编译引擎。
     * @param tokenizer 已初始化的JackTokenizer实例。
     * @param outputFile 输出XML文件的路径。
     * @throws IOException 如果无法创建或写入输出文件。
     */
    public CompilationEngine(JackTokenizer tokenizer, String outputFile) throws IOException {
        this.tokenizer = tokenizer;
        this.writer = new PrintWriter(new FileWriter(outputFile));
        this.tokenizer.advance(); // 加载第一个词法单元以启动解析
    }

    /**
     * 关闭输出文件写入器，确保所有内容都已写入磁盘。
     */
    public void close() {
        writer.close();
    }

    /**
     * 写入一行带当前缩进的XML。
     * @param s 要写入的字符串。
     */
    private void write(String s) {
        writer.println("  ".repeat(indent) + s);
    }

    /**
     * 处理并写入当前词法单元的XML表示，然后将词法分析器前进到下一个。
     * 这是处理所有终结符（terminals）的核心方法。
     */
    private void processCurrentToken() {
        JackTokenizer.TokenType type = tokenizer.tokenType();
        String tag = "";
        String value = "";

        switch(type) {
            case KEYWORD:
                tag = "keyword";
                value = tokenizer.keyword();
                break;
            case SYMBOL:
                tag = "symbol";
                char symbol = tokenizer.symbol();
                value = SYMBOL_MAP.getOrDefault(symbol, String.valueOf(symbol));
                break;
            case IDENTIFIER:
                tag = "identifier";
                value = tokenizer.identifier();
                break;
            case INT_CONST:
                tag = "integerConstant";
                value = String.valueOf(tokenizer.intVal());
                break;
            case STRING_CONST:
                tag = "stringConstant";
                value = tokenizer.stringVal();
                break;
        }

        write("<" + tag + "> " + value + " </" + tag + ">");
        tokenizer.advance();
    }

    // =======================================================================
    // 编译例程 (Compilation Routines)
    // 每个方法对应Jack语法中的一个非终结符 (non-terminal)。
    // =======================================================================

    /**
     * 编译一个完整的类。
     * 语法: 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() {
        write("<class>");
        indent++;
        processCurrentToken(); // 'class'
        processCurrentToken(); // className
        processCurrentToken(); // '{'

        // 编译类变量声明 (static, field)
        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
                (tokenizer.keyword().equals("static") || tokenizer.keyword().equals("field"))) {
            compileClassVarDec();
        }
        // 编译子程序 (constructor, function, method)
        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
                (tokenizer.keyword().equals("constructor") || tokenizer.keyword().equals("function") || tokenizer.keyword().equals("method"))) {
            compileSubroutine();
        }

        processCurrentToken(); // '}'
        indent--;
        write("</class>");
    }

    /**
     * 编译一个静态变量或字段声明。
     * 语法: ('static' | 'field') type varName (',' varName)* ';'
     */
    public void compileClassVarDec() {
        write("<classVarDec>");
        indent++;
        processCurrentToken(); // 'static' or 'field'
        processCurrentToken(); // type (int, char, boolean, className)
        processCurrentToken(); // varName
        while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
            processCurrentToken(); // ','
            processCurrentToken(); // varName
        }
        processCurrentToken(); // ';'
        indent--;
        write("</classVarDec>");
    }

    /**
     * 编译一个完整的方法、函数或构造函数。
     * 语法: ('constructor'|'function'|'method') ('void'|type) subroutineName '(' parameterList ')' subroutineBody
     */
    public void compileSubroutine() {
        write("<subroutineDec>");
        indent++;
        processCurrentToken(); // 'constructor', 'function', or 'method'
        processCurrentToken(); // 'void' or type
        processCurrentToken(); // subroutineName
        processCurrentToken(); // '('
        compileParameterList();
        processCurrentToken(); // ')'
        compileSubroutineBody();
        indent--;
        write("</subroutineDec>");
    }

    /**
     * 编译参数列表（可能为空）。
     * 语法: ((type varName) (',' type varName)*)?
     */
    public void compileParameterList() {
        write("<parameterList>");
        indent++;
        // 检查参数列表是否为空 (下一个词法单元是 ')' )
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            processCurrentToken(); // type
            processCurrentToken(); // varName
            while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
                processCurrentToken(); // ','
                processCurrentToken(); // type
                processCurrentToken(); // varName
            }
        }
        indent--;
        write("</parameterList>");
    }

    /**
     * 编译子程序体。
     * 语法: '{' varDec* statements '}'
     */
    public void compileSubroutineBody() {
        write("<subroutineBody>");
        indent++;
        processCurrentToken(); // '{'
        // 编译局部变量声明
        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyword().equals("var")) {
            compileVarDec();
        }
        compileStatements();
        processCurrentToken(); // '}'
        indent--;
        write("</subroutineBody>");
    }

    /**
     * 编译一个 'var' 声明。
     * 语法: 'var' type varName (',' varName)* ';'
     */
    public void compileVarDec() {
        write("<varDec>");
        indent++;
        processCurrentToken(); // 'var'
        processCurrentToken(); // type
        processCurrentToken(); // varName
        while(tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ','){
            processCurrentToken(); // ','
            processCurrentToken(); // varName
        }
        processCurrentToken(); // ';'
        indent--;
        write("</varDec>");
    }

    /**
     * 编译一系列语句。
     * 语法: statement*
     */
    public void compileStatements() {
        write("<statements>");
        indent++;
        // 循环直到遇到非语句开头的关键字或符号 '}'
        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            switch (tokenizer.keyword()) {
                case "let": compileLet(); break;
                case "if": compileIf(); break;
                case "while": compileWhile(); break;
                case "do": compileDo(); break;
                case "return": compileReturn(); break;
                default:
                    // 如果不是语句开头的关键字，则退出循环
                    indent--;
                    write("</statements>");
                    return;
            }
        }
        indent--;
        write("</statements>");
    }

    /**
     * 编译 'let' 语句。
     * 语法: 'let' varName ('[' expression ']')? '=' expression ';'
     */
    public void compileLet() {
        write("<letStatement>");
        indent++;
        processCurrentToken(); // 'let'
        processCurrentToken(); // varName
        // 检查是否有数组访问
        if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '[') {
            processCurrentToken(); // '['
            compileExpression();
            processCurrentToken(); // ']'
        }
        processCurrentToken(); // '='
        compileExpression();
        processCurrentToken(); // ';'
        indent--;
        write("</letStatement>");
    }

    /**
     * 编译 'if' 语句，可能带有 'else' 子句。
     * 语法: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     */
    public void compileIf() {
        write("<ifStatement>");
        indent++;
        processCurrentToken(); // 'if'
        processCurrentToken(); // '('
        compileExpression();
        processCurrentToken(); // ')'
        processCurrentToken(); // '{'
        compileStatements();
        processCurrentToken(); // '}'
        // 检查是否有 'else' 子句
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyword().equals("else")) {
            processCurrentToken(); // 'else'
            processCurrentToken(); // '{'
            compileStatements();
            processCurrentToken(); // '}'
        }
        indent--;
        write("</ifStatement>");
    }

    /**
     * 编译 'while' 语句。
     * 语法: 'while' '(' expression ')' '{' statements '}'
     */
    public void compileWhile() {
        write("<whileStatement>");
        indent++;
        processCurrentToken(); // 'while'
        processCurrentToken(); // '('
        compileExpression();
        processCurrentToken(); // ')'
        processCurrentToken(); // '{'
        compileStatements();
        processCurrentToken(); // '}'
        indent--;
        write("</whileStatement>");
    }

    /**
     * 编译 'do' 语句。
     * 语法: 'do' subroutineCall ';'
     */
    public void compileDo() {
        write("<doStatement>");
        indent++;
        processCurrentToken(); // 'do'
        compileTerm(); // 'do'语句的核心是一个子程序调用，它本身是一个term
        processCurrentToken(); // ';'
        indent--;
        write("</doStatement>");
    }

    /**
     * 编译 'return' 语句。
     * 语法: 'return' expression? ';'
     */
    public void compileReturn() {
        write("<returnStatement>");
        indent++;
        processCurrentToken(); // 'return'
        // 如果下一个词法单元不是';'，说明有返回值表达式
        if (!(tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ';')) {
            compileExpression();
        }
        processCurrentToken(); // ';'
        indent--;
        write("</returnStatement>");
    }

    /**
     * 编译一个表达式。
     * 语法: term (op term)*
     */
    public void compileExpression() {
        write("<expression>");
        indent++;
        compileTerm();
        // 循环处理 (op term) 部分
        while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && "+-*/&|<>=".indexOf(tokenizer.symbol()) != -1) {
            processCurrentToken(); // op
            compileTerm();
        }
        indent--;
        write("</expression>");
    }

    /**
     * 编译一个 term。这是表达式中最复杂的部分，因为它有多种可能性。
     * 语法: integerConstant | stringConstant | keywordConstant | varName |
     * varName'['expression']' | subroutineCall | '('expression')' | unaryOp term
     */
    public void compileTerm() {
        write("<term>");
        indent++;
        // 使用向前看（lookahead）来区分不同类型的term
        String nextToken = tokenizer.peek();

        if (tokenizer.tokenType() == JackTokenizer.TokenType.IDENTIFIER) {
            if (nextToken != null && nextToken.equals("[")) { // 数组访问: varName'['expression']'
                processCurrentToken(); // varName
                processCurrentToken(); // '['
                compileExpression();
                processCurrentToken(); // ']'
            } else if (nextToken != null && (nextToken.equals("(") || nextToken.equals("."))) { // 子程序调用
                // subroutineName'('expressionList')' 或 className|varName'.'subroutineName'('expressionList')'
                processCurrentToken(); // subroutineName or className/varName
                if (nextToken.equals(".")) {
                    processCurrentToken(); // '.'
                    processCurrentToken(); // subroutineName
                }
                processCurrentToken(); // '('
                compileExpressionList();
                processCurrentToken(); // ')'
            } else { // 普通变量: varName
                processCurrentToken();
            }
        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '(') { // '('expression')'
            processCurrentToken(); // '('
            compileExpression();
            processCurrentToken(); // ')'
        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')) { // unaryOp term
            processCurrentToken(); // unaryOp
            compileTerm();
        } else { // 整数、字符串、关键字常量
            processCurrentToken();
        }
        indent--;
        write("</term>");
    }

    /**
     * 编译一个（可能为空的）逗号分隔的表达式列表。
     * 用于子程序调用。
     * 语法: (expression (',' expression)*)?
     */
    public void compileExpressionList() {
        write("<expressionList>");
        indent++;
        // 检查表达式列表是否为空 (下一个词法单元是 ')' )
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            compileExpression();
            while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
                processCurrentToken(); // ','
                compileExpression();
            }
        }
        indent--;
        write("</expressionList>");
    }
}