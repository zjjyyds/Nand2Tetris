package Nand2Tetris.proj6.HackAssembler;

import java.util.HashMap;

/**
 * Code 类：将 Hack 汇编语言的 C 指令字段（dest、comp、jump）翻译成二进制代码。
 * 实现 C 指令的二进制编码：111 a c1 c2 c3 c4 c5 c6 d1 d2 d3 j1 j2 j3
 */
public class Code {
    // 存储 dest 字段的助记符到二进制的映射
    private static final HashMap<String, String> DEST_MAP = new HashMap<>();
    // 存储 comp 字段的助记符到二进制的映射
    private static final HashMap<String, String> COMP_MAP = new HashMap<>();
    // 存储 jump 字段的助记符到二进制的映射
    private static final HashMap<String, String> JUMP_MAP = new HashMap<>();

    // 静态初始化块：初始化 dest、comp 和 jump 的映射表
    static {
        // 初始化 dest 映射表（3 位，8 种组合）
        // 空字符串表示不存储结果，M 表示存储到 M，D 表示存储到 D，MD 表示同时存储到 M 和 D，等等
        DEST_MAP.put("", "000");   // 000: 不存储
        DEST_MAP.put("M", "001");  // 001: 存储到 M
        DEST_MAP.put("D", "010");  // 010: 存储到 D
        DEST_MAP.put("MD", "011"); // 011: 存储到 M 和 D
        DEST_MAP.put("A", "100");  // 100: 存储到 A
        DEST_MAP.put("AM", "101"); // 101: 存储到 A 和 M
        DEST_MAP.put("AD", "110"); // 110: 存储到 A 和 D
        DEST_MAP.put("AMD", "111");// 111: 存储到 A、M 和 D
        // 兼容 DM 和 ADM（Project 6 文档中的已知问题）
        DEST_MAP.put("DM", "011"); // DM 等价于 MD
        DEST_MAP.put("ADM", "111");// ADM 等价于 AMD

        // 初始化 comp 映射表（7 位，28 种 ALU 运算）
        // 格式：a c1 c2 c3 c4 c5 c6
        // a 位决定使用 A 还是 M（0 表示 A，1 表示 M）
        // c1-c6 控制 ALU 运算
        COMP_MAP.put("0", "0101010");   // 0
        COMP_MAP.put("1", "0111111");   // 1
        COMP_MAP.put("-1", "0111010");  // -1
        COMP_MAP.put("D", "0001100");   // D
        COMP_MAP.put("A", "0110000");   // A
        COMP_MAP.put("!D", "0001101");  // !D
        COMP_MAP.put("!A", "0110001");  // !A
        COMP_MAP.put("-D", "0001111");  // -D
        COMP_MAP.put("-A", "0110011");  // -A
        COMP_MAP.put("D+1", "0011111"); // D+1
        COMP_MAP.put("A+1", "0110111"); // A+1
        COMP_MAP.put("D-1", "0001110"); // D-1
        COMP_MAP.put("A-1", "0110010"); // A-1
        COMP_MAP.put("D+A", "0000010"); // D+A
        COMP_MAP.put("D-A", "0010011"); // D-A
        COMP_MAP.put("A-D", "0000111"); // A-D
        COMP_MAP.put("D&A", "0000000"); // D&A
        COMP_MAP.put("D|A", "0010101"); // D|A
        // 当 a=1 时，使用 M 替代 A
        COMP_MAP.put("M", "1110000");   // M
        COMP_MAP.put("!M", "1110001");  // !M
        COMP_MAP.put("-M", "1110011");  // -M
        COMP_MAP.put("M+1", "1110111"); // M+1
        COMP_MAP.put("M-1", "1110010"); // M-1
        COMP_MAP.put("D+M", "1000010"); // D+M
        COMP_MAP.put("D-M", "1010011"); // D-M
        COMP_MAP.put("M-D", "1000111"); // M-D
        COMP_MAP.put("D&M", "1000000"); // D&M
        COMP_MAP.put("D|M", "1010101"); // D|M

        // 初始化 jump 映射表（3 位，8 种组合）
        // 空字符串表示不跳转，JGT 表示大于时跳转，JEQ 表示等于时跳转，等等
        JUMP_MAP.put("", "000");   // 000: 不跳转
        JUMP_MAP.put("JGT", "001"); // 001: 大于 0 时跳转
        JUMP_MAP.put("JEQ", "010"); // 010: 等于 0 时跳转
        JUMP_MAP.put("JGE", "011"); // 011: 大于等于 0 时跳转
        JUMP_MAP.put("JLT", "100"); // 100: 小于 0 时跳转
        JUMP_MAP.put("JNE", "101"); // 101: 不等于 0 时跳转
        JUMP_MAP.put("JLE", "110"); // 110: 小于等于 0 时跳转
        JUMP_MAP.put("JMP", "111"); // 111: 无条件跳转
    }
    /**
     * 将 C 指令的 dest 助记符翻译为 3 位二进制代码。
     * @param mnemonic dest 助记符（例如 "M", "D", "MD" 等）
     * @return 3 位二进制字符串；如果助记符无效，返回 "000"
     */
    public String dest(String mnemonic) {
        // 查找映射表，获取对应的二进制代码
        return DEST_MAP.getOrDefault(mnemonic, "000");
    }
    /**
     * 将 C 指令的 comp 助记符翻译为 7 位二进制代码。
     * @param mnemonic comp 助记符（例如 "D+A", "M", "0" 等）
     * @return 7 位二进制字符串；如果助记符无效，返回 "0000000"
     */
    public String comp(String mnemonic) {
        // 查找映射表，获取对应的二进制代码
        return COMP_MAP.getOrDefault(mnemonic, "0000000");
    }
    /**
     * 将 C 指令的 jump 助记符翻译为 3 位二进制代码。
     * @param mnemonic jump 助记符（例如 "JGT", "JEQ", "JMP" 等）
     * @return 3 位二进制字符串；如果助记符无效，返回 "000"
     */
    public String jump(String mnemonic) {
        // 查找映射表，获取对应的二进制代码
        return JUMP_MAP.getOrDefault(mnemonic, "000");
    }
}
