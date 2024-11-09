package dev.cesarc.tinj;

import dev.cesarc.tinj.syntax.nodes.Expr;
import dev.cesarc.tinj.syntax.nodes.Stmt;
import dev.cesarc.tinj.token.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * The Resolver class is responsible for resolving variable declarations and scopes
 * within the given statements and expressions. It implements the Visitor pattern
 * for both expressions and statements.
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    /**
     * The interpreter to use for resolving.
     */
    private final Interpreter interpreter;

    /**
     * The stack of scopes, where each scope is a map of variable names to whether
     * they have been defined or not yet.
     */
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    private FunctionType currentFunction = FunctionType.NONE;

    /**
     * Constructs a Resolver with the given interpreter.
     *
     * @param interpreter The interpreter to use for resolving.
     */
    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }

    /**
     * Visits a block statement, beginning a new scope, resolving the statements
     * within the block, and then ending the scope.
     *
     * @param stmt The block statement to visit.
     * @return null
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        declare(stmt.name);
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    /**
     * Visits a function statement, declaring the function, defining the function,
     * and then resolving the function.
     *
     * @param stmt The function statement to visit.
     * @return null
     */
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Main.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            resolve(stmt.value);
        }

        return null;
    }

    /**
     * Visits a variable statement, declaring the variable, resolving its initializer
     * if present, and then defining the variable.
     *
     * @param stmt The variable statement to visit.
     * @return null
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        // Declare the variable in the current scope
        declare(stmt.name);

        // Resolve the initializer if present
        // This is done before defining the variable to prevent the variable from being read in its own initializer
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }

        // Define the variable in the current scope
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    /**
     * Visits an assignment expression, resolving the value being assigned and the
     * variable being assigned to.
     *
     * @param expr The assignment expression to visit.
     * @return null
     */
    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    /**
     * Visits a variable expression, ensuring the variable is not read within its
     * own initializer and resolving the variable locally.
     *
     * @param expr The variable expression to visit.
     * @return null
     */
    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Main.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    /**
     * Resolves a list of statements.
     *
     * @param statements The list of statements to resolve.
     */
    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    /**
     * Resolves a single statement by accepting the visitor.
     *
     * @param stmt The statement to resolve.
     */
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * Resolves a single expression by accepting the visitor.
     *
     * @param expr The expression to resolve.
     */
    private void resolve(Expr expr) {
        expr.accept(this);
    }

    /**
     * Resolves a function by beginning a new scope, declaring and defining the
     * function's parameters, resolving the function's body, and then ending the scope.
     *
     * @param function The function to resolve.
     */
    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();

        for (Token param : function.params) {
            declare(param);
            define(param);
        }

        resolve(function.body);

        endScope();
        currentFunction = enclosingFunction;
    }

    /**
     * Begins a new scope by pushing a new empty map onto the scope stack.
     */
    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    /**
     * Ends the current scope by popping the map off the scope stack.
     */
    private void endScope() {
        scopes.pop();
    }

    /**
     * Declares a variable in the current scope.
     *
     * @param name The token representing the variable name.
     */
    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();

        if (scope.containsKey(name.lexeme)) {
            Main.error(name, "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme, false);
    }

    /**
     * Defines a variable in the current scope.
     *
     * @param name The token representing the variable name.
     */
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    /**
     * Resolves a variable locally by checking the scopes stack and informing
     * the interpreter of the variable's scope distance.
     *
     * @param expr The variable expression to resolve.
     * @param name The token representing the variable name.
     */
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }
}