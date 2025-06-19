package JackAnalyzer;

// JackAnalyzer.java
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Jack语法分析器的主驱动程序。
 * 该程序接收一个命令行参数，该参数可以是一个.jack文件名，或一个包含一个或多个.jack文件的目录名。
 * 对于每个指定的.jack文件，它会创建一个JackTokenizer和一个CompilationEngine，
 * 并驱动它们生成对应的.xml输出文件。
 */
public class JackAnalyzer {

    /**
     * main方法，程序的入口点。
     * @param args 命令行参数。应包含一个.jack文件或一个目录的路径。
     */
    public static void main(String[] args) {
        // 1. 检查命令行参数是否正确
        if (args.length != 1) {
            System.out.println("使用方法: java JackAnalyzer [file.jack | directory]");
            return;
        }

        File input = new File(args[0]);
        List<File> jackFiles = new ArrayList<>();

        // 2. 判断输入是文件还是目录，并收集所有需要处理的.jack文件
        if (input.isDirectory()) {
            // 如果是目录，找到所有.jack文件
            File[] files = input.listFiles((dir, name) -> name.endsWith(".jack"));
            if (files != null) {
                for (File file : files) {
                    jackFiles.add(file);
                }
            }
        } else if (input.isFile() && input.getName().endsWith(".jack")) {
            // 如果是单个.jack文件，直接添加
            jackFiles.add(input);
        } else {
            System.out.println("错误: 输入必须是一个.jack文件或一个包含.jack文件的目录。");
            return;
        }

        if (jackFiles.isEmpty()) {
            System.out.println("未找到需要分析的.jack文件。");
            return;
        }

        // 3. 遍历所有找到的.jack文件并进行分析
        for (File jackFile : jackFiles) {
            // 根据输入文件名构造输出文件名，例如 "Main.jack" -> "Main.xml"
            String outputFilePath = jackFile.getAbsolutePath().replace(".jack", ".xml");
            System.out.println("正在分析: " + jackFile.getPath() + " -> " + outputFilePath);

            CompilationEngine engine = null;
            try {
                // 为每个文件创建新的Tokenizer和Engine
                JackTokenizer tokenizer = new JackTokenizer(jackFile.getAbsolutePath());
                engine = new CompilationEngine(tokenizer, outputFilePath);

                // 启动编译过程（从顶层的'class'规则开始）
                engine.compileClass();

                System.out.println("成功生成: " + outputFilePath);
            } catch (IOException e) {
                // 处理文件读写错误
                System.err.println("处理文件时发生错误 " + jackFile.getPath() + ": " + e.getMessage());
                e.printStackTrace();
            } catch (IllegalStateException e){
                // 处理语法分析过程中抛出的语法错误
                System.err.println("文件 " + jackFile.getPath() + " 中存在语法错误: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 确保无论成功还是失败，文件写入器都被关闭
                if (engine != null) {
                    engine.close();
                }
            }
        }
    }
}