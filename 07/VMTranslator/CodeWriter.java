package VMTranslator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CodeWriter {
    private BufferedWriter writer;
    private String fileName; // 当前处理的 VM 文件名（用于 static 段）
    private int labelCounter; // 用于生成唯一标签（如 eq, gt）

    public CodeWriter(String outputFile) throws IOException {
        writer = new BufferedWriter(new FileWriter(outputFile));
        labelCounter = 0;
    }

    // 设置当前文件名（用于 static 段符号）
    public void setFileName(String fileName) {
        this.fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    }

    // 写入算术命令
    public void writeArithmetic(String command) throws IOException {
        if (command.equals("add")) {
            writer.write("// add\n");
            writer.write("@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nM=D+M\n@SP\nM=M+1\n");
        } else if (command.equals("sub")) {
            writer.write("// sub\n");
            writer.write("@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nM=M-D\n@SP\nM=M+1\n");
        } else if (command.equals("neg")) {
            writer.write("// neg\n");
            writer.write("@SP\nA=M-1\nM=-M\n");
        } else if (command.equals("eq")) {
            String label = "EQ_" + labelCounter++;
            writer.write("// eq\n");
            writer.write("@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nD=M-D\n@SET_TRUE_" + label + "\nD;JEQ\n");
            writer.write("@SP\nA=M\nM=0\n@END_" + label + "\n0;JMP\n");
            writer.write("(SET_TRUE_" + label + ")\n@SP\nA=M\nM=-1\n");
            writer.write("(END_" + label + ")\n@SP\nM=M+1\n");
        } else if (command.equals("gt")) {
            String label = "GT_" + labelCounter++;
            writer.write("// gt\n");
            writer.write("@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nD=M-D\n@SET_TRUE_" + label + "\nD;JGT\n");
            writer.write("@SP\nA=M\nM=0\n@END_" + label + "\n0;JMP\n");
            writer.write("(SET_TRUE_" + label + ")\n@SP\nA=M\nM=-1\n");
            writer.write("(END_" + label + ")\n@SP\nM=M+1\n");
        } else if (command.equals("lt")) {
            String label = "LT_" + labelCounter++;
            writer.write("// lt\n");
            writer.write("@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nD=M-D\n@SET_TRUE_" + label + "\nD;JLT\n");
            writer.write("@SP\nA=M\nM=0\n@END_" + label + "\n0;JMP\n");
            writer.write("(SET_TRUE_" + label + ")\n@SP\nA=M\nM=-1\n");
            writer.write("(END_" + label + ")\n@SP\nM=M+1\n");
        } else if (command.equals("and")) {
            writer.write("// and\n");
            writer.write("@SP\nAM=M-1\nD=M\n@SP\nA=M-1\nM=D&M\n");
        } else if (command.equals("or")) {
            writer.write("// or\n");
            writer.write("@SP\nAM=M-1\nD=M\n@SP\nA=M-1\nM=D|M\n");
        } else if (command.equals("not")) {
            writer.write("// not\n");
            writer.write("@SP\nA=M-1\nM=!M\n");
        } else {
            throw new IllegalArgumentException("Unsupported arithmetic command: " + command);
        }
    }

    // 写入 push/pop 命令
    public void writePushPop(Parser.CommandType commandType, String segment, int index) throws IOException {
        if (commandType == Parser.CommandType.C_PUSH) {
            if (segment.equals("constant")) {
                writer.write("// push constant " + index + "\n");
                writer.write("@" + index + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("local")) {
                writer.write("// push local " + index + "\n");
                writer.write("@LCL\nD=M\n@" + index + "\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("argument")) {
                writer.write("// push argument " + index + "\n");
                writer.write("@ARG\nD=M\n@" + index + "\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("this")) {
                writer.write("// push this " + index + "\n");
                writer.write("@THIS\nD=M\n@" + index + "\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("that")) {
                writer.write("// push that " + index + "\n");
                writer.write("@THAT\nD=M\n@" + index + "\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("temp")) {
                writer.write("// push temp " + index + "\n");
                writer.write("@" + (5 + index) + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("pointer")) {
                writer.write("// push pointer " + index + "\n");
                writer.write("@" + (3 + index) + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("static")) {
                writer.write("// push static " + index + "\n");
                writer.write("@" + fileName + "." + index + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            }
        } else if (commandType == Parser.CommandType.C_POP) {
            if (segment.equals("local")) {
                writer.write("// pop local " + index + "\n");
                writer.write("@LCL\nD=M\n@" + index + "\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
            } else if (segment.equals("argument")) {
                writer.write("// pop argument " + index + "\n");
                writer.write("@ARG\nD=M\n@" + index + "\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
            } else if (segment.equals("this")) {
                writer.write("// pop this " + index + "\n");
                writer.write("@THIS\nD=M\n@" + index + "\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
            } else if (segment.equals("that")) {
                writer.write("// pop that " + index + "\n");
                writer.write("@THAT\nD=M\n@" + index + "\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
            } else if (segment.equals("temp")) {
                writer.write("// pop temp " + index + "\n");
                writer.write("@SP\nAM=M-1\nD=M\n@" + (5 + index) + "\nM=D\n");
            } else if (segment.equals("pointer")) {
                writer.write("// pop pointer " + index + "\n");
                writer.write("@SP\nAM=M-1\nD=M\n@" + (3 + index) + "\nM=D\n");
            } else if (segment.equals("static")) {
                writer.write("// pop static " + index + "\n");
                writer.write("@SP\nAM=M-1\nD=M\n@" + fileName + "." + index + "\nM=D\n");
            } else {
                throw new IllegalArgumentException("Pop not supported for segment: " + segment);
            }
        }
    }

    // 关闭输出文件
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}