package proj11;
// VMWriter.java
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * VM写入器模块。
 * 提供了一系列简洁的API，用于将VM命令写入到输出文件。
 * 这将代码生成逻辑与文件写入细节分离开来。
 */
public class VMWriter {

    /**
     * 定义VM中的内存段。
     * 注意：Jack语言的 `field` 对应VM的 `this` 段。
     */
    public enum Segment {
        CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP
    }

    /**
     * 定义VM支持的算术/逻辑命令。
     */
    public enum Command {
        ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT
    }

    private PrintWriter writer;

    /**
     * 创建一个新的VM写入器，准备写入到指定的输出文件。
     * @param outputFile 输出的 .vm 文件路径
     * @throws IOException 如果文件创建失败
     */
    public VMWriter(String outputFile) throws IOException {
        this.writer = new PrintWriter(new FileWriter(outputFile));
    }

    /**
     * 写入一个VM push命令。
     * @param segment 内存段 (CONST, ARG, LOCAL, etc.)
     * @param index   段内的索引
     */
    public void writePush(Segment segment, int index) {
        writer.println("push " + segmentToString(segment) + " " + index);
    }

    /**
     * 写入一个VM pop命令。
     * @param segment 内存段 (ARG, LOCAL, THIS, etc.)
     * @param index   段内的索引
     */
    public void writePop(Segment segment, int index) {
        writer.println("pop " + segmentToString(segment) + " " + index);
    }

    // 辅助方法，将Segment枚举转换为VM命令中的字符串
    private String segmentToString(Segment segment) {
        switch(segment) {
            case CONST: return "constant";
            case ARG: return "argument";
            default: return segment.toString().toLowerCase();
        }
    }

    /**
     * 写入一个算术或逻辑命令。
     * @param command 命令 (ADD, SUB, NOT, etc.)
     */
    public void writeArithmetic(Command command) {
        writer.println(command.toString().toLowerCase());
    }

    /**
     * 写入一个VM label命令。
     * @param label 标签字符串
     */
    public void writeLabel(String label) {
        writer.println("label " + label);
    }

    /**
     * 写入一个VM goto命令。
     * @param label 要跳转到的标签
     */
    public void writeGoto(String label) {
        writer.println("goto " + label);
    }

    /**
     * 写入一个VM if-goto命令。
     * @param label 如果栈顶值为-1(true)，则跳转到该标签
     */
    public void writeIf(String label) {
        writer.println("if-goto " + label);
    }

    /**
     * 写入一个VM call命令。
     * @param name 要调用的函数名 (e.g., "ClassName.subroutineName")
     * @param nArgs 传递给函数的参数数量
     */
    public void writeCall(String name, int nArgs) {
        writer.println("call " + name + " " + nArgs);
    }

    /**
     * 写入一个VM function命令。
     * @param name 函数名
     * @param nLocals 函数的局部变量数量
     */
    public void writeFunction(String name, int nLocals) {
        writer.println("function " + name + " " + nLocals);
    }

    /**
     * 写入一个VM return命令。
     */
    public void writeReturn() {
        writer.println("return");
    }

    /**
     * 关闭输出文件，确保所有缓冲区的命令都被写入。
     */
    public void close() {
        writer.close();
    }
}