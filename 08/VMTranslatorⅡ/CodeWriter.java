package VMTranslatorⅡ;

import VMTranslatorⅡ.Parser;

import java.io.*;

public class CodeWriter implements AutoCloseable {
    private PrintWriter writer;
    private String fileName;
    private int labelCounter;
    private String currentFunction;

    public CodeWriter(File output) throws IOException {
        writer = new PrintWriter(new FileWriter(output));
        labelCounter = 0;
        currentFunction = "";
    }

    public void setFileName(String fileName) {
        this.fileName = fileName.replace(".vm", "");
    }

    public void writeBootstrap() {
        writeComment("Bootstrap code");
        writer.println("@256");
        writer.println("D=A");
        writer.println("@SP");
        writer.println("M=D"); // SP = 256
        writeCall("Sys.init", 0);
    }

    public void writeArithmetic(String command) {
        writeComment(command);
        if (command.equals("add") || command.equals("sub") || command.equals("and") || command.equals("or")) {
            writeBinaryOperation(command);
        } else if (command.equals("neg") || command.equals("not")) {
            writeUnaryOperation(command);
        } else if (command.equals("eq") || command.equals("gt") || command.equals("lt")) {
            writeComparison(command);
        }
    }

    private void writeBinaryOperation(String command) {
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M"); // D = y
        writer.println("A=A-1"); // Point to x
        if (command.equals("add")) {
            writer.println("M=D+M"); // 修正为 D+M
        } else if (command.equals("sub")) {
            writer.println("M=M-D");
        } else if (command.equals("and")) {
            writer.println("M=M&D");
        } else if (command.equals("or")) {
            writer.println("M=M|D");
        }
    }

    private void writeUnaryOperation(String command) {
        writer.println("@SP");
        writer.println("A=M-1");
        if (command.equals("neg")) {
            writer.println("M=-M");
        } else if (command.equals("not")) {
            writer.println("M=!M");
        }
    }

    private void writeComparison(String command) {
        String labelTrue = "TRUE_" + labelCounter;
        String labelEnd = "END_" + labelCounter++;
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M"); // D = y
        writer.println("A=A-1"); // Point to x
        writer.println("D=M-D"); // x - y
        writer.println("@" + labelTrue);
        if (command.equals("eq")) {
            writer.println("D;JEQ");
        } else if (command.equals("gt")) {
            writer.println("D;JGT");
        } else if (command.equals("lt")) {
            writer.println("D;JLT");
        }
        writer.println("@SP");
        writer.println("A=M-1");
        writer.println("M=0"); // False
        writer.println("@" + labelEnd);
        writer.println("0;JMP");
        writer.println("(" + labelTrue + ")");
        writer.println("@SP");
        writer.println("A=M-1");
        writer.println("M=-1"); // True
        writer.println("(" + labelEnd + ")");
    }

    public void writePushPop(Parser.CommandType command, String segment, int index) {
        writeComment(command + " " + segment + " " + index);
        if (command == Parser.CommandType.C_PUSH) {
            if (segment.equals("constant")) {
                writer.println("@" + index);
                writer.println("D=A");
                pushDToStack();
            } else if (segment.equals("local") || segment.equals("argument") || segment.equals("this") || segment.equals("that")) {
                writePushSegment(segment, index);
            } else if (segment.equals("static")) {
                writer.println("@" + fileName + "." + index);
                writer.println("D=M");
                pushDToStack();
            } else if (segment.equals("temp")) {
                writer.println("@5");
                writer.println("D=A");
                writer.println("@" + index);
                writer.println("A=D+A");
                writer.println("D=M");
                pushDToStack();
            } else if (segment.equals("pointer")) {
                writer.println(index == 0 ? "@THIS" : "@THAT");
                writer.println("D=M");
                pushDToStack();
            }
        } else if (command == Parser.CommandType.C_POP) {
            if (segment.equals("local") || segment.equals("argument") || segment.equals("this") || segment.equals("that")) {
                writePopSegment(segment, index);
            } else if (segment.equals("static")) {
                popFromStackToD();
                writer.println("@" + fileName + "." + index);
                writer.println("M=D");
            } else if (segment.equals("temp")) {
                popFromStackToD();
                writer.println("@5");
                writer.println("D=A");
                writer.println("@" + index);
                writer.println("A=D+A");
                writer.println("M=D");
            } else if (segment.equals("pointer")) {
                popFromStackToD();
                writer.println(index == 0 ? "@THIS" : "@THAT");
                writer.println("M=D");
            }
        }
    }

    private void writePushSegment(String segment, int index) {
        String seg = segment.equals("local") ? "LCL" : segment.equals("argument") ? "ARG" : segment.equals("this") ? "THIS" : "THAT";
        writer.println("@" + seg);
        writer.println("D=M");
        writer.println("@" + index);
        writer.println("A=D+A");
        writer.println("D=M");
        pushDToStack();
    }

    private void writePopSegment(String segment, int index) {
        String seg = segment.equals("local") ? "LCL" : segment.equals("argument") ? "ARG" : segment.equals("this") ? "THIS" : "THAT";
        writer.println("@" + seg);
        writer.println("D=M");
        writer.println("@" + index);
        writer.println("D=D+A");
        writer.println("@R13");
        writer.println("M=D"); // Store address in R13
        popFromStackToD();
        writer.println("@R13");
        writer.println("A=M");
        writer.println("M=D");
    }

    private void pushDToStack() {
        writer.println("@SP");
        writer.println("A=M");
        writer.println("M=D");
        writer.println("@SP");
        writer.println("M=M+1");
    }

    private void popFromStackToD() {
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M");
    }

    public void writeLabel(String label) {
        writeComment("label " + label);
        writer.println("(" + currentFunction + "$" + label + ")");
    }

    public void writeGoto(String label) {
        writeComment("goto " + label);
        writer.println("@" + currentFunction + "$" + label);
        writer.println("0;JMP");
    }

    public void writeIf(String label) {
        writeComment("if-goto " + label);
        popFromStackToD();
        writer.println("@" + currentFunction + "$" + label);
        writer.println("D;JNE");
    }

    public void writeFunction(String functionName, int nVars) {
        writeComment("function " + functionName + " " + nVars);
        currentFunction = functionName;
        writer.println("(" + functionName + ")");
        for (int i = 0; i < nVars; i++) {
            writer.println("@0");
            writer.println("D=A");
            pushDToStack();
        }
    }

    public void writeCall(String functionName, int nArgs) {
        writeComment("call " + functionName + " " + nArgs);
        String returnAddress = functionName + "$ret." + labelCounter++;
        writer.println("@" + returnAddress);
        writer.println("D=A");
        pushDToStack();
        for (String seg : new String[]{"LCL", "ARG", "THIS", "THAT"}) {
            writer.println("@" + seg);
            writer.println("D=M");
            pushDToStack();
        }
        writer.println("@SP");
        writer.println("D=M");
        writer.println("@5");
        writer.println("D=D-A");
        writer.println("@" + nArgs);
        writer.println("D=D-A");
        writer.println("@ARG");
        writer.println("M=D");
        writer.println("@SP");
        writer.println("D=M");
        writer.println("@LCL");
        writer.println("M=D");
        writer.println("@" + functionName);
        writer.println("0;JMP");
        writer.println("(" + returnAddress + ")");
    }

    public void writeReturn() {
        writeComment("return");
        writer.println("@LCL");
        writer.println("D=M");
        writer.println("@R13");
        writer.println("M=D"); // endFrame
        writer.println("@5");
        writer.println("A=D-A");
        writer.println("D=M");
        writer.println("@R14");
        writer.println("M=D"); // retAddr
        popFromStackToD();
        writer.println("@ARG");
        writer.println("A=M");
        writer.println("M=D");
        writer.println("@ARG");
        writer.println("D=M+1");
        writer.println("@SP");
        writer.println("M=D");
        for (int i = 1; i <= 4; i++) {
            String seg = i == 1 ? "THAT" : i == 2 ? "THIS" : i == 3 ? "ARG" : "LCL";
            writer.println("@R13");
            writer.println("D=M");
            writer.println("@" + i);
            writer.println("A=D-A");
            writer.println("D=M");
            writer.println("@" + seg);
            writer.println("M=D");
        }
        writer.println("@R14");
        writer.println("A=M");
        writer.println("0;JMP");
    }

    private void writeComment(String comment) {
        writer.println("// " + comment);
    }

    @Override
    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}