package dev.cesarc.tinj.syntax.nodes;

import dev.cesarc.tinj.token.Token;

import java.util.List;

public abstract class Stmt {
    /// A method for visiting each type of statement
    public interface Visitor<R> {
        /// Visit a block statement
        R visitBlockStmt(Block stmt);

        /// Visit a class statement
        R visitClassStmt(Class stmt);

        /// Visit an expression statement
        R visitExpressionStmt(Expression stmt);

        /// Visit a function statement
        R visitFunctionStmt(Function stmt);

        /// Visit an if statement
        R visitIfStmt(If stmt);

        /// Visit a print statement
        R visitPrintStmt(Print stmt);

        /// Visit a return statement
        R visitReturnStmt(Return stmt);

        /// Visit a variable declaration statement
        R visitVarStmt(Var stmt);

        /// Visit a while statement
        R visitWhileStmt(While stmt);
    }

    public static class Block extends Stmt {
        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }

        public final List<Stmt> statements;
    }

    /// A statement defining a new class
    public static class Class extends Stmt {
        /**
         * Create a new class statement with the provided name, superclass, and methods
         * @param name The name of the class
         * @param methods The methods of the class
         */
        public Class(Token name, List<Stmt.Function> methods) {
            this.name = name;
            //this.superclass = superclass;
            this.methods = methods;
        }

        /// Make the provided visitor visit this statement
        ///
        /// @param visitor The visitor to visit this statement
        /// @return The result of visiting this statement
        /// @see Stmt.Visitor
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }

        /**
         * The name of the class
         */
        public final Token name;

        //public final Expr.Variable superclass;

        /**
         * The methods of the class
         */
        public final List<Stmt.Function> methods;
    }

    public static class Expression extends Stmt {
        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }

        public final Expr expression;
    }

    public static class Function extends Stmt {
        public Function(Token name, List<Token> params, Stmt body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }

        public final Token name;
        public final List<Token> params;
        public final Stmt body;
    }

    public static class If extends Stmt {
        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }

        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;
    }

    public static class Print extends Stmt {
        public Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }

        public final Expr expression;
    }

    public static class Return extends Stmt {
        public Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }

        public final Token keyword;
        public final Expr value;
    }

    public static class Var extends Stmt {
        public Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }

        public final Token name;
        public final Expr initializer;
    }

    public static class While extends Stmt {
        public While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }

        public final Expr condition;
        public final Stmt body;
    }

    public abstract <R> R accept(Visitor<R> visitor);
}
