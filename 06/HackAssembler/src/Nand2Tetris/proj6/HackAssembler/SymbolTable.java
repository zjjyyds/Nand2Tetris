package Nand2Tetris.proj6.HackAssembler;

import java.util.HashMap;

/**
 * SymbolTable 类：管理 Hack 汇编语言中的符号与地址的映射。
 * 包括预定义符号（如 R0-R15、SP、SCREEN 等）、标签和变量。
 * 用于汇编器的符号解析，支持第一次扫描（添加标签）和第二次扫描（处理变量）。
 */
public class SymbolTable {
    // 存储符号到地址的映射
    private HashMap<String, Integer> symbolTable;
    // 用于分配变量地址，从 16 开始递增
    private int variableAddress;
    /**
     * 构造函数：初始化符号表，添加 Hack 语言的预定义符号。
     */
    public SymbolTable() {
        symbolTable = new HashMap<>();
        variableAddress = 16; // 变量地址从 16 开始分配（Hack 语言规范）

        // 初始化预定义符号（根据 Hack 语言规范）
        // 寄存器符号 R0-R15 映射到地址 0-15
        for (int i = 0; i <= 15; i++) {
            symbolTable.put("R" + i, i);
        }
        // 栈指针 SP 映射到地址 0
        symbolTable.put("SP", 0);
        // 本地变量指针 LCL 映射到地址 1
        symbolTable.put("LCL", 1);
        // 参数指针 ARG 映射到地址 2
        symbolTable.put("ARG", 2);
        // this 指针 THIS 映射到地址 3
        symbolTable.put("THIS", 3);
        // that 指针 THAT 映射到地址 4
        symbolTable.put("THAT", 4);
        // 屏幕内存起始地址 SCREEN 映射到 16384
        symbolTable.put("SCREEN", 16384);
        // 键盘内存地址 KBD 映射到 24576
        symbolTable.put("KBD", 24576);
    }
    /**
     * 添加符号和对应的地址到符号表。
     * 通常用于第一次扫描时添加标签（L_COMMAND）。
     * @param symbol 符号名（例如标签名）
     * @param address 符号对应的地址（通常是 ROM 地址）
     */
    public void addEntry(String symbol, int address) {
        symbolTable.put(symbol, address);
    }
    /**
     * 检查符号是否存在于符号表中。
     * @param symbol 要检查的符号名
     * @return 如果符号存在，返回 true；否则返回 false
     */
    public boolean contains(String symbol) {
        return symbolTable.containsKey(symbol);
    }
    /**
     * 获取符号对应的地址。
     * @param symbol 符号名
     * @return 符号对应的地址；如果符号不存在，返回 -1
     */
    public int getAddress(String symbol) {
        return symbolTable.getOrDefault(symbol, -1);
    }
    /**
     * 为新变量分配地址，并添加到符号表。
     * 变量地址从 16 开始递增（variableAddress）。
     * 如果变量已存在，直接返回其地址。
     * @param variable 变量名（例如 A_COMMAND 中的符号）
     * @return 变量的地址
     */
    public int allocateVariableAddress(String variable) {
        if (!contains(variable)) {
            // 如果变量不存在，分配新地址并添加到符号表
            symbolTable.put(variable, variableAddress);
            return variableAddress++;
        }
        // 如果变量已存在，返回其地址
        return getAddress(variable);
    }
}
