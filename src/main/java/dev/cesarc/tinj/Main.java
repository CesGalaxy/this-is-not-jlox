package dev.cesarc.tinj;

import dev.cesarc.tinj.parser.Parser;
import dev.cesarc.tinj.syntax.nodes.Stmt;
import dev.cesarc.tinj.token.Scanner;
import dev.cesarc.tinj.token.Token;
import dev.cesarc.tinj.token.TokenType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * The Main class is the entry point for the Tinj interpreter. It reads the source code
 * from a file or the REPL, scans the code into tokens, parses the tokens into statements,
 * and then interprets the statements.
 * <p>
 * The Main class also provides methods for reporting errors in the source code and
 * runtime errors that occur during interpretation.
 *
 * @see Scanner
 * @see Parser
 * @see Resolver
 * @see Interpreter
 *
 * @since Always
 * @version Current
 * @author NoSoyCésar
 */
public class Main {
    /// The interpreter instance
    private static final Interpreter interpreter = new Interpreter();

    /// Whether there was a syntax error in the code
    static boolean hadError = false;
    /// Whether there was a runtime error in the code
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            // Show usage message
            System.out.println("Usage: tinj [script]");
            System.exit(64);
        } else if (args.length == 1) {
            // Run a file with the code
            runFile(args[0]);
        } else {
            // Run the REPL
            runPrompt();
        }
    }

    /// Execute the contents of the file at the provided path
    ///
    /// @param path The path to the file to run
    /// @throws IOException If the file could not be read
    private static void runFile(String path) throws IOException {
        // Read the file
        byte[] bytes = Files.readAllBytes(Paths.get(path));

        // Run it (may set hadError to true)
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error (if the program ended with one) in the exit code
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /// Start listening to prompts from the user and run them in the REPL
    ///
    /// @throws IOException If there was an error reading the input
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            if (line.equals("quit")) System.exit(0);
            run(line);

            // Reset the error flag
            hadError = false;
        }
    }

    /// Run the provided source code
    ///
    /// @param source The source code to run
    private static void run(String source) {
        // Scan the code and get the tokens from it
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // Parse the tokens and get the statements from them
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a resolution error.
        if (hadError) return;

        // Run the code in the current session interpreter
        interpreter.interpret(statements);
    }

    /// Report an error in the source code
    ///
    /// @param line    The line where the error occurred
    /// @param message The error message
    public static void error(int line, String message) {
        report(line, "", message);
    }

    /// Report an error in the scanned tokens
    ///
    /// @param token   The token where the error occurred
    /// @param message The error message
    public static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    /// Report an error while reading or executing any kind of instruction
    ///
    /// @param line    The line where the error occurred
    /// @param where   The location of the error
    /// @param message The error message
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
    }

    /// Report a runtime error
    ///
    /// @param error The runtime error that occurred
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}