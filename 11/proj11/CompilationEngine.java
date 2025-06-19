package proj11;

// CompilationEngine.java
import proj11.JackTokenizer;
import proj11.SymbolTable;
import proj11.VMWriter;

import java.io.IOException;

/**
 * 编译引擎（代码生成器版本）。
 * 负责解析Jack语法结构，并利用SymbolTable和VMWriter生成等效的VM代码。
 */
public class CompilationEngine {
    private JackTokenizer tokenizer;
    private SymbolTable symbolTable;
    private VMWriter vmWriter;
    private String className;
    private int labelCounter = 0;

    /**
     * 构造函数，初始化编译引擎。
     */
    public CompilationEngine(JackTokenizer tokenizer, SymbolTable symbolTable, VMWriter vmWriter) throws IOException {
        this.tokenizer = tokenizer;
        this.symbolTable = symbolTable;
        this.vmWriter = vmWriter;
        tokenizer.advance(); // 加载第一个词法单元
    }

    // 辅助方法，用于生成唯一的标签名
    private String newLabel() {
        return "L" + (labelCounter++);
    }

    // 辅助方法，用于处理并消耗一个期望的词法单元
    private void process(String value) {
        tokenizer.advance();
    }

    // --- 编译例程 ---

    /**
     * 编译一个完整的类。
     * 语法: 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() {
        process("class");
        this.className = tokenizer.identifier();
        process(className);
        process("{");

        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && (tokenizer.keyword().equals("static") || tokenizer.keyword().equals("field"))) {
            compileClassVarDec();
        }
        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && (tokenizer.keyword().equals("constructor") || tokenizer.keyword().equals("function") || tokenizer.keyword().equals("method"))) {
            compileSubroutine();
        }

        process("}");
    }

    /**
     * 编译一个静态变量或字段声明。
     * 这个阶段只填充符号表，不生成VM代码。
     */
    public void compileClassVarDec() {
        SymbolTable.Kind kind = SymbolTable.Kind.valueOf(tokenizer.keyword().toUpperCase());
        process(tokenizer.keyword()); // static or field
        String type = tokenizer.getCurrentToken();
        process(type); // type
        symbolTable.define(tokenizer.identifier(), type, kind);
        process(tokenizer.identifier()); // varName

        while (tokenizer.symbol() == ',') {
            process(",");
            symbolTable.define(tokenizer.identifier(), type, kind);
            process(tokenizer.identifier());
        }
        process(";");
    }

    /**
     * 编译一个完整的方法、函数或构造函数。
     */
    public void compileSubroutine() {
        symbolTable.startSubroutine(); // 开始新的子程序作用域
        String subroutineType = tokenizer.keyword();
        process(subroutineType);

        if (subroutineType.equals("method")) {
            // 为方法的'this'参数在符号表中添加一项
            symbolTable.define("this", className, SymbolTable.Kind.ARG);
        }

        process(tokenizer.getCurrentToken()); // returnType
        String subroutineName = className + "." + tokenizer.identifier();
        process(tokenizer.identifier()); // subroutineName

        process("(");
        compileParameterList();
        process(")");

        compileSubroutineBody(subroutineName, subroutineType);
    }

    /**
     * 编译参数列表，填充符号表。
     */
    public void compileParameterList() {
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            String type = tokenizer.getCurrentToken();
            process(type);
            symbolTable.define(tokenizer.identifier(), type, SymbolTable.Kind.ARG);
            process(tokenizer.identifier());

            while (tokenizer.symbol() == ',') {
                process(",");
                type = tokenizer.getCurrentToken();
                process(type);
                symbolTable.define(tokenizer.identifier(), type, SymbolTable.Kind.ARG);
                process(tokenizer.identifier());
            }
        }
    }

    /**
     * 编译子程序体。
     */
    public void compileSubroutineBody(String name, String type) {
        process("{");
        // 编译局部变量声明，并填充符号表
        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyword().equals("var")) {
            compileVarDec();
        }

        // 写入function命令，指明局部变量数量
        vmWriter.writeFunction(name, symbolTable.varCount(SymbolTable.Kind.VAR));

        // 为构造函数和方法生成初始化代码
        if (type.equals("constructor")) {
            // 分配内存
            vmWriter.writePush(VMWriter.Segment.CONST, symbolTable.varCount(SymbolTable.Kind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            // 将'this'指向新分配的内存地址
            vmWriter.writePop(VMWriter.Segment.POINTER, 0);
        } else if (type.equals("method")) {
            // 将第一个参数（隐式的'this'）设置为当前对象的基地址
            vmWriter.writePush(VMWriter.Segment.ARG, 0);
            vmWriter.writePop(VMWriter.Segment.POINTER, 0);
        }

        compileStatements();
        process("}");
    }

    /**
     * 编译一个var声明，填充符号表。
     */
    public void compileVarDec() {
        process("var");
        String type = tokenizer.getCurrentToken();
        process(type);
        symbolTable.define(tokenizer.identifier(), type, SymbolTable.Kind.VAR);
        process(tokenizer.identifier());
        while (tokenizer.symbol() == ',') {
            process(",");
            symbolTable.define(tokenizer.identifier(), type, SymbolTable.Kind.VAR);
            process(tokenizer.identifier());
        }
        process(";");
    }

    /**
     * 编译一系列语句。
     */
    public void compileStatements() {
        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            switch (tokenizer.keyword()) {
                case "let": compileLet(); break;
                case "if": compileIf(); break;
                case "while": compileWhile(); break;
                case "do": compileDo(); break;
                case "return": compileReturn(); break;
                default: return; // 非语句，结束
            }
        }
    }

    /**
     * 编译let语句。
     */
    public void compileLet() {
        process("let");
        String varName = tokenizer.identifier();
        process(varName);

        boolean isArray = tokenizer.symbol() == '[';
        if (isArray) { // 处理数组赋值 let arr[expr1] = expr2;
            process("[");
            compileExpression(); // 计算偏移量 expr1
            process("]");

            pushVar(varName); // push数组基地址
            vmWriter.writeArithmetic(VMWriter.Command.ADD); // 计算目标地址 (base + offset)

            process("=");
            compileExpression(); // 计算右侧表达式 expr2
            process(";");

            // 将结果存入目标地址
            vmWriter.writePop(VMWriter.Segment.TEMP, 0);    // 暂存expr2的结果
            vmWriter.writePop(VMWriter.Segment.POINTER, 1); // 将目标地址存入THAT指针
            vmWriter.writePush(VMWriter.Segment.TEMP, 0);    // 将结果压回栈
            vmWriter.writePop(VMWriter.Segment.THAT, 0);     // 存入THAT指向的内存位置
        } else { // 处理普通变量赋值
            process("=");
            compileExpression();
            process(";");
            popVar(varName); // 将表达式结果存入变量
        }
    }

    /**
     * 编译if语句。
     */
    public void compileIf() {
        String elseLabel = newLabel();
        String endLabel = newLabel();

        process("if");
        process("(");
        compileExpression(); // 计算条件
        process(")");

        vmWriter.writeArithmetic(VMWriter.Command.NOT); // if (not condition) goto L_ELSE
        vmWriter.writeIf(elseLabel);

        process("{");
        compileStatements(); // if代码块
        process("}");
        vmWriter.writeGoto(endLabel); // 执行完if后，跳转到结尾

        vmWriter.writeLabel(elseLabel); // else代码块的标签
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyword().equals("else")) {
            process("else");
            process("{");
            compileStatements();
            process("}");
        }
        vmWriter.writeLabel(endLabel); // if语句的结束标签
    }

    /**
     * 编译while语句。
     */
    public void compileWhile() {
        String startLabel = newLabel();
        String endLabel = newLabel();

        vmWriter.writeLabel(startLabel); // 循环开始的标签
        process("while");
        process("(");
        compileExpression(); // 计算循环条件
        process(")");

        vmWriter.writeArithmetic(VMWriter.Command.NOT); // if (not condition) goto L_END
        vmWriter.writeIf(endLabel);

        process("{");
        compileStatements(); // 循环体
        process("}");
        vmWriter.writeGoto(startLabel); // 返回循环开始处
        vmWriter.writeLabel(endLabel); // 循环结束的标签
    }

    /**
     * 编译do语句。
     */
    public void compileDo() {
        process("do");
        compileTerm(); // do语句的核心就是一个子程序调用
        process(";");
        vmWriter.writePop(VMWriter.Segment.TEMP, 0); // 丢弃返回值 (void函数返回值是0)
    }

    /**
     * 编译return语句。
     */
    public void compileReturn() {
        process("return");
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ';') {
            compileExpression(); // 如果有返回值，计算它
        } else {
            vmWriter.writePush(VMWriter.Segment.CONST, 0); // void方法默认返回0
        }
        vmWriter.writeReturn();
        process(";");
    }

    /**
     * 编译表达式。
     */
    public void compileExpression() {
        compileTerm();
        // 处理 (op term)* 部分
        while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && "+-*/&|<>=".indexOf(tokenizer.symbol()) != -1) {
            String op = String.valueOf(tokenizer.symbol());
            process(op);
            compileTerm();
            // 为操作符生成相应的VM命令
            switch (op) {
                case "+": vmWriter.writeArithmetic(VMWriter.Command.ADD); break;
                case "-": vmWriter.writeArithmetic(VMWriter.Command.SUB); break;
                case "*": vmWriter.writeCall("Math.multiply", 2); break;
                case "/": vmWriter.writeCall("Math.divide", 2); break;
                case "&": vmWriter.writeArithmetic(VMWriter.Command.AND); break;
                case "|": vmWriter.writeArithmetic(VMWriter.Command.OR); break;
                case "<": vmWriter.writeArithmetic(VMWriter.Command.LT); break;
                case ">": vmWriter.writeArithmetic(VMWriter.Command.GT); break;
                case "=": vmWriter.writeArithmetic(VMWriter.Command.EQ); break;
            }
        }
    }

    /**
     * 编译一个term，这是表达式中最复杂的部分。
     */
    public void compileTerm() {
        JackTokenizer.TokenType type = tokenizer.tokenType();
        String currentToken = tokenizer.getCurrentToken();

        if (type == JackTokenizer.TokenType.INT_CONST) {
            vmWriter.writePush(VMWriter.Segment.CONST, tokenizer.intVal());
            process(currentToken);
        } else if (type == JackTokenizer.TokenType.STRING_CONST) {
            String str = tokenizer.stringVal();
            vmWriter.writePush(VMWriter.Segment.CONST, str.length());
            vmWriter.writeCall("String.new", 1);
            for (char c : str.toCharArray()) {
                vmWriter.writePush(VMWriter.Segment.CONST, c);
                vmWriter.writeCall("String.appendChar", 2);
            }
            process(currentToken);
        } else if (type == JackTokenizer.TokenType.KEYWORD) {
            switch (tokenizer.keyword()) {
                case "true":
                    vmWriter.writePush(VMWriter.Segment.CONST, 0);
                    vmWriter.writeArithmetic(VMWriter.Command.NOT);
                    break;
                case "false":
                case "null":
                    vmWriter.writePush(VMWriter.Segment.CONST, 0);
                    break;
                case "this":
                    vmWriter.writePush(VMWriter.Segment.POINTER, 0);
                    break;
            }
            process(currentToken);
        } else if (type == JackTokenizer.TokenType.SYMBOL) {
            if (currentToken.equals("(")) { // (expression)
                process("(");
                compileExpression();
                process(")");
            } else { // unaryOp term
                process(currentToken);
                compileTerm();
                if (currentToken.equals("-")) {
                    vmWriter.writeArithmetic(VMWriter.Command.NEG);
                } else { // ~
                    vmWriter.writeArithmetic(VMWriter.Command.NOT);
                }
            }
        } else { // IDENTIFIER
            String nextToken = tokenizer.peek();
            if (nextToken != null && nextToken.equals("[")) { // 数组访问 var[expr]
                String varName = tokenizer.identifier();
                process(varName);
                process("[");
                compileExpression();
                process("]");
                pushVar(varName); // push数组基地址
                vmWriter.writeArithmetic(VMWriter.Command.ADD); // 计算目标地址
                vmWriter.writePop(VMWriter.Segment.POINTER, 1); // 将目标地址设为THAT
                vmWriter.writePush(VMWriter.Segment.THAT, 0);   // 将数组元素压栈
            } else if (nextToken != null && (nextToken.equals("(") || nextToken.equals("."))) { // 子程序调用
                compileCall();
            } else { // 普通变量
                pushVar(currentToken);
                process(currentToken);
            }
        }
    }

    /**
     * 编译一个子程序调用。
     */
    private void compileCall() {
        String first = tokenizer.identifier();
        process(first);
        int nArgs = 0;
        String callName;

        if (tokenizer.symbol() == '.') { // 形式: obj.method() 或 Class.func()
            process(".");
            String second = tokenizer.identifier();
            process(second);

            SymbolTable.Kind kind = symbolTable.kindOf(first);
            if(kind != SymbolTable.Kind.NONE) { // 是一个对象实例: obj.method()
                nArgs = 1; // 增加'this'参数
                pushVar(first); // 将对象基地址作为第一个参数压栈
                callName = symbolTable.typeOf(first) + "." + second;
            } else { // 是一个类名: Class.func()
                callName = first + "." + second;
            }

        } else { // 形式: method()，在当前对象上调用
            nArgs = 1; // 增加'this'参数
            vmWriter.writePush(VMWriter.Segment.POINTER, 0); // 将当前对象的'this'压栈
            callName = className + "." + first;
        }

        process("(");
        nArgs += compileExpressionList(); // 编译参数列表并获取参数数量
        process(")");

        vmWriter.writeCall(callName, nArgs);
    }

    /**
     * 编译一个表达式列表。
     * @return 列表中的表达式数量
     */
    public int compileExpressionList() {
        int count = 0;
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            compileExpression();
            count = 1;
            while (tokenizer.symbol() == ',') {
                process(",");
                compileExpression();
                count++;
            }
        }
        return count;
    }

    // 辅助方法，用于将变量值压入栈
    private void pushVar(String name) {
        SymbolTable.Kind kind = symbolTable.kindOf(name);
        int index = symbolTable.indexOf(name);
        switch(kind){
            case STATIC: vmWriter.writePush(VMWriter.Segment.STATIC, index); break;
            case FIELD: vmWriter.writePush(VMWriter.Segment.THIS, index); break;
            case ARG: vmWriter.writePush(VMWriter.Segment.ARG, index); break;
            case VAR: vmWriter.writePush(VMWriter.Segment.LOCAL, index); break;
        }
    }

    // 辅助方法，用于将栈顶值弹出到变量
    private void popVar(String name) {
        SymbolTable.Kind kind = symbolTable.kindOf(name);
        int index = symbolTable.indexOf(name);
        switch(kind){
            case STATIC: vmWriter.writePop(VMWriter.Segment.STATIC, index); break;
            case FIELD: vmWriter.writePop(VMWriter.Segment.THIS, index); break;
            case ARG: vmWriter.writePop(VMWriter.Segment.ARG, index); break;
            case VAR: vmWriter.writePop(VMWriter.Segment.LOCAL, index); break;
        }
    }
}