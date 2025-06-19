package proj11;
// JackAnalyzer.java
import proj11.CompilationEngine;
import proj11.JackTokenizer;
import proj11.SymbolTable;
import proj11.VMWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Jack编译器的主驱动程序。
 * 负责协调Tokenizer, SymbolTable, VMWriter, 和 CompilationEngine来完成编译。
 * 它可以处理单个.jack文件或整个目录。
 */
public class JackAnalyzer {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("使用方法: java JackAnalyzer [file.jack | directory]");
            return;
        }

        File input = new File(args[0]);
        List<File> jackFiles = new ArrayList<>();

        if (input.isDirectory()) {
            File[] files = input.listFiles((dir, name) -> name.endsWith(".jack"));
            if (files != null) {
                for (File file : files) {
                    jackFiles.add(file);
                }
            }
        } else if (input.isFile() && input.getName().endsWith(".jack")) {
            jackFiles.add(input);
        }

        if (jackFiles.isEmpty()) {
            System.out.println("未找到需要编译的.jack文件。");
            return;
        }

        for (File jackFile : jackFiles) {
            // 根据输入文件名构造输出文件名，例如 "Main.jack" -> "Main.vm"
            String outputFilePath = jackFile.getAbsolutePath().replace(".jack", ".vm");
            System.out.println("正在编译: " + jackFile.getPath() + " -> " + outputFilePath);

            VMWriter vmWriter = null;
            try {
                // 为每个文件创建全新的实例
                JackTokenizer tokenizer = new JackTokenizer(jackFile.getAbsolutePath());
                SymbolTable symbolTable = new SymbolTable();
                vmWriter = new VMWriter(outputFilePath);
                CompilationEngine engine = new CompilationEngine(tokenizer, symbolTable, vmWriter);

                // 从顶层的 'class' 规则开始编译
                engine.compileClass();

                System.out.println("成功编译: " + outputFilePath);

            } catch (IOException e) {
                System.err.println("处理文件时发生错误 " + jackFile.getPath() + ": " + e.getMessage());
            } catch (Exception e) {
                System.err.println("在 " + jackFile.getPath() + " 中发生编译错误: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 确保无论成功还是失败，文件写入器都被正确关闭
                if (vmWriter != null) {
                    vmWriter.close();
                }
            }
        }
    }
}