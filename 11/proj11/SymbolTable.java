package proj11;

// SymbolTable.java
import java.util.HashMap;
import java.util.Map;

/**
 * 符号表模块。
 * 负责管理所有标识符（变量、参数、类名、子程序名等）的作用域、类型、种类和索引。
 * 符号表包含两个作用域：类作用域和子程序作用域。
 */
public class SymbolTable {

    /**
     * 定义标识符的种类（或作用域）。
     * STATIC: 静态变量
     * FIELD: 字段变量 (属于对象)
     * ARG: 参数
     * VAR: 局部变量
     * NONE: 未找到
     */
    public enum Kind {
        STATIC, FIELD, ARG, VAR, NONE
    }

    // 存储类级别符号 (static, field)
    private Map<String, SymbolInfo> classSymbols;
    // 存储子程序级别符号 (arg, var)
    private Map<String, SymbolInfo> subroutineSymbols;
    // 为每个种类维护一个运行索引计数器
    private Map<Kind, Integer> indexCounters;

    /**
     * 内部类，用于存储每个符号的详细信息。
     */
    private static class SymbolInfo {
        String type; // 类型 (e.g., "int", "Square")
        Kind kind;   // 种类 (STATIC, FIELD, ARG, VAR)
        int index;   // 索引

        SymbolInfo(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    /**
     * 创建一个新的、空的符号表。
     */
    public SymbolTable() {
        classSymbols = new HashMap<>();
        subroutineSymbols = new HashMap<>();
        indexCounters = new HashMap<>();
        indexCounters.put(Kind.STATIC, 0);
        indexCounters.put(Kind.FIELD, 0);
        indexCounters.put(Kind.ARG, 0);
        indexCounters.put(Kind.VAR, 0);
    }

    /**
     * 开始一个新的子程序作用域。
     * 在编译一个新的方法、函数或构造函数时调用此方法，
     * 它会清空旧的子程序符号表并重置参数和局部变量的索引计数器。
     */
    public void startSubroutine() {
        subroutineSymbols.clear();
        indexCounters.put(Kind.ARG, 0);
        indexCounters.put(Kind.VAR, 0);
    }

    /**
     * 向符号表中添加一个新的标识符，并为其分配一个基于其种类的运行索引。
     * @param name 标识符名称
     * @param type 标识符类型
     * @param kind 标识符种类 (STATIC, FIELD, ARG, VAR)
     */
    public void define(String name, String type, Kind kind) {
        int index = indexCounters.get(kind);
        SymbolInfo info = new SymbolInfo(type, kind, index);

        if (kind == Kind.STATIC || kind == Kind.FIELD) {
            classSymbols.put(name, info);
        } else { // ARG or VAR
            subroutineSymbols.put(name, info);
        }

        // 增加对应种类的索引计数器
        indexCounters.put(kind, index + 1);
    }

    /**
     * 返回在当前作用域中定义的给定种类的变量数量。
     * @param kind 种类 (STATIC, FIELD, ARG, VAR)
     * @return 该种类的变量数量
     */
    public int varCount(Kind kind) {
        return indexCounters.get(kind);
    }

    /**
     * 返回在当前作用域中找到的具名标识符的种类。
     * 它会首先查找子程序作用域，如果未找到，再查找类作用域。
     * @param name 标识符名称
     * @return 标识符的种类，如果未找到则返回 Kind.NONE
     */
    public Kind kindOf(String name) {
        if (subroutineSymbols.containsKey(name)) {
            return subroutineSymbols.get(name).kind;
        }
        if (classSymbols.containsKey(name)) {
            return classSymbols.get(name).kind;
        }
        return Kind.NONE;
    }

    /**
     * 返回具名标识符的类型。
     * @param name 标识符名称
     * @return 标识符的类型 (e.g., "int")
     */
    public String typeOf(String name) {
        if (subroutineSymbols.containsKey(name)) {
            return subroutineSymbols.get(name).type;
        }
        if (classSymbols.containsKey(name)) {
            return classSymbols.get(name).type;
        }
        return ""; // 在一个语法正确的程序中不应发生
    }

    /**
     * 返回具名标识符的索引。
     * @param name 标识符名称
     * @return 标识符的索引
     */
    public int indexOf(String name) {
        if (subroutineSymbols.containsKey(name)) {
            return subroutineSymbols.get(name).index;
        }
        if (classSymbols.containsKey(name)) {
            return classSymbols.get(name).index;
        }
        return -1; // 不应发生
    }
}