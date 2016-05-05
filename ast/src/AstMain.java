import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.io.*;
import java.util.*;

public class AstMain {

    private static final String PROJECT_NAME = "k9mail";
    private static final File PROJECT_SRC = new File(PROJECT_NAME, "src");
    private static final File PROJECT_LIBS = new File(PROJECT_NAME, "libs");
    private static final File PROJECT_CLASSES = new File(PROJECT_NAME, "classes");

    public static void main(String args[]) {

        // Check if we need to add a pause before parsing (ideal for attaching a profiler)
        boolean doPause = false;
        if (args.length > 0 && "--pause".equals(args[0])) {
            doPause = true;
        }

        // Find the source files
        List<String> files = new ArrayList<>();
        scanJavaFiles(PROJECT_SRC, files);
        String [] sources = files.toArray(new String [0]);
        String [] encodings = new String [sources.length];
        for (int i = 0; i < encodings.length; ++i) {
            encodings[i] = "UTF-8";
        }

        // The classpath is ${PROJECT_NAME}/classses/ + ${PROJECT_NAME}/libs/*.jar
        File [] jars = PROJECT_LIBS.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".jar");
            }
        });
        String [] classpaths = new String [jars.length + 1];
        classpaths[0] = PROJECT_CLASSES.toString();
        for (int i = 0; i < jars.length; ++i) {
            classpaths[i + 1] = jars[i].toString();
        }


        // Accept all compilation units
        final Map<String, CompilationUnit> compilationUnits = new HashMap<>();
        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String source, CompilationUnit ast) {
                compilationUnits.put(source, ast);
            }
        };


        // Parse all source files (create the ASTs, this is slow, specially if bindings are requested)
        long start = System.currentTimeMillis();
        ASTParser parser = createAstParser(classpaths, new String [] { PROJECT_SRC.toString() });
        long end = System.currentTimeMillis();
        System.out.printf("Created ast parser in %,d ms\n", end - start);

        // Add a pause for the profiler
        if (doPause) {
            try {
                System.out.print("Press enter to exit the program: ");
                System.out.flush();
                //noinspection ResultOfMethodCallIgnored
                System.in.read();
                System.out.println("Ending program");
            }
            catch (IOException e) {
                // Ignored
            }
        }

        start = System.currentTimeMillis();
        parser.createASTs(sources, encodings, new String [0], requestor, null);
        end = System.currentTimeMillis();
        System.out.printf("Parsed %s files in %,d ms\n", compilationUnits.size(), end - start);


        // Print the errors
        int errors = 0;
        for (Map.Entry<String, CompilationUnit> entrySet : compilationUnits.entrySet()) {
            String fileName = entrySet.getKey();
            CompilationUnit compilationUnit = entrySet.getValue();

            IProblem[] problems = compilationUnit.getProblems();
            if (problems == null || problems.length == 0) {
                continue;
            }

            boolean showFile = true;
            for (IProblem problem : problems) {
                if (!problem.isError()) {
                    continue;
                }

                if (showFile) {
                    System.out.printf("%s\n", fileName);
                    showFile = false;
                }

                System.out.printf("  %s\n", problem);
                ++errors;
            }
        }

        System.out.printf("Found %,d errors in %s\n", errors, PROJECT_NAME);
    }

    // Create an AST parser for java7
    @SuppressWarnings("deprecation")
    private static ASTParser createAstParser(String [] classpaths, String [] sources) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setStatementsRecovery(true);
        Map<String, String> options = new HashMap<>();
        options.put(JavaCore.COMPILER_SOURCE, "1.7");
        options.put(JavaCore.COMPILER_COMPLIANCE, "1.7");
        parser.setCompilerOptions(options);

        // A parser with bindings, this makes the inspection accurate but slower
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setEnvironment(
            classpaths,
            sources,
            null, // Use platform default encoding
            true // include vm boot
        );
        return parser;
    }

    // Find the java source files by scanning the top folder
    private static void scanJavaFiles(File file, Collection<String> files) {
        if (file.isDirectory()) {
            for (String subFileName : file.list()) {
                if (subFileName.startsWith(".")) {
                    continue;
                }
                File subFile = new File(file, subFileName);
                scanJavaFiles(subFile, files);
            }
        } else {
            String fileName = file.toString();
            if (fileName.endsWith(".java")) {
                files.add(fileName);
            }
        }
    }
}
