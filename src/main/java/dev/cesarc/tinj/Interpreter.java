package dev.cesarc.tinj;

import dev.cesarc.tinj.lang.LangCallable;
import dev.cesarc.tinj.lang.LangClass;
import dev.cesarc.tinj.lang.LangFunction;
import dev.cesarc.tinj.lang.LangInstance;
import dev.cesarc.tinj.syntax.nodes.Expr;
import dev.cesarc.tinj.syntax.nodes.Stmt;
import dev.cesarc.tinj.token.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.cesarc.tinj.token.TokenType.OR;

/**
 * The VM for the language, it interprets the AST and executes its instructions
 *
 * @see dev.cesarc.tinj.syntax.nodes.Expr
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    /// The global environment for the interpreter, it's constant
    public final Environment globals = new Environment();

    /// The environment currently being used
    private Environment environment = globals;

    /// The local variables in the current scope
    private final Map<Expr, Integer> locals = new HashMap<>();

    /// Create a new interpreter and define the native functions
    Interpreter() {
        globals.define("now", System.currentTimeMillis() / 1000.0);
        globals.define("clock", new LangCallable() {
            @Override
            public int arity() {
                return 0;
            }

            /// Get the current time in seconds
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            // Function.prototype.constants.nativeFnIdentifier = "<native fn>"

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    /**
     * Execute a list of statements
     *
     * @param statements The statements to execute
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        return switch (expr.operator.type) {
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    yield (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    yield (String) left + (String) right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
            case GREATER -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left >= (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left <= (double) right;
            }
            case DASH -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left - (double) right;
            }
            case SLASH -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left / (double) right;
            }
            case ASTERISK -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left * (double) right;
            }
            case BANG_EQUAL -> !isEqual(left, right);
            case EQUAL_EQUAL -> isEqual(left, right);
            default -> throw new RuntimeException("Unreachable");
        };
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        if (!(callee instanceof LangCallable function)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }


        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LangInstance instance) {
            return instance.get(expr.name);
        }

        throw new RuntimeError(expr.name,
                "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (object instanceof LangInstance instance) {
            Object value = evaluate(expr.value);
            instance.set(expr.name, value);
            return value;
        } else {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case DASH:
                if (right instanceof Double)
                    return -(double) right;
                if (right instanceof String)
                    return new StringBuilder((String) right).reverse().toString();
                if (right instanceof Boolean)
                    return !((boolean) right);

                throw new RuntimeError(expr.operator, "Invalid operand type for unary -");
            case BANG:
                return !isTruthy(right);
            default:
                throw new RuntimeException("Unreachable");
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    /**
     * Check if two objects are equal
     *
     * @param a The first object
     * @param b The second object
     * @return Whether the objects are equal
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    /**
     * Convert data of any type to a string
     *
     * @param object The object to convert
     * @return The string representation of the object
     */
    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
            return text;
        }

        return object.toString();
    }

    /**
     * Evaluate an expression and return the result of the evaluation
     *
     * @param expr The expression to evaluate
     * @return The result of the evaluation
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /**
     * Execute a statement
     *
     * @param stmt The statement to execute
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    public void executeStmt(Stmt statement, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;
            execute(statement);
        } finally {
            this.environment = previous;
        }
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        Map<String, LangFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LangFunction function = new LangFunction(method, environment);
            methods.put(method.name.lexeme, function);
        }

        LangClass klass = new LangClass(stmt.name.lexeme, methods);

        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LangFunction function = new LangFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new ReturnValue(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }
}
