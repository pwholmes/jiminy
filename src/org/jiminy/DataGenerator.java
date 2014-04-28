package org.jiminy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import org.jiminy.domain.DataType;
import org.jiminy.domain.Operator;
import org.jiminy.domain.Value;
import org.jiminy.domain.expression.BinaryExpression;
import org.jiminy.domain.expression.ConstantExpression;
import org.jiminy.domain.expression.DecisionListExpression;
import org.jiminy.domain.expression.Expression;
import org.jiminy.domain.expression.VariableExpression;

public class DataGenerator {
   private static final int RANDOM_THRESHOLD = 3;
   
   static Random generator = new Random();

   public static HashMap<String,Value> generateSymbolTable(int numVariables) {
      DataType[] types = DataType.values();
      
      // Create the specifed number of variables and assign them random values
      HashMap<String,Value> symbolTable = new HashMap<String,Value>();
      for (int i = 1; i <= numVariables; i++) {
         String varName = "var" + i;
         DataType type = types[generator.nextInt(types.length)];
         Value value = Value.generateRandomValue(generator, type);
         symbolTable.put(varName, value);
      }
      
      return symbolTable;
   }

   public static ArrayList<Expression> generateExpressions(HashMap<String,Value> symbolTable, int numExpressions, int maxExpressionComplexity) {
      ArrayList<Expression> expressions = new ArrayList<Expression>();
      
      // Get the set of variable names from the symbol table
      int numVars = symbolTable.size();
      String[] varNames = new String[numVars];
      symbolTable.keySet().toArray(varNames);
      
      // Get the set of operators
      Operator[] operators = Operator.values();
      
      // Create the specified number of expressions using the variables in the symbol table
      for (int i = 1; i <= numExpressions; i++) {
         DecisionListExpression firstNode = null;
         DecisionListExpression lastNode = null;
         
         // Each expression has a random number of decision list nodes
         int dlLength = generator.nextInt(maxExpressionComplexity) + 1;
         for (int j = 1; j <= dlLength; j++) {
            // Each decision list node is a binary expression coupled with a boolean value.
            // Each binary expression is (in this case) a variable, a comparison operator,
            // and a constant value.
            // Select a variable from the symbol table
            String varName = varNames[generator.nextInt(numVars)];
            Expression operand1 = new VariableExpression(varName);
            
            // Select a value to compare against.  For some percentage of the expressions, use 
            // the variable's actual value in the symbol table, for the rest generate a random value. 
            Value value = symbolTable.get(varName);
            DataType type = value.getType();
            Expression operand2 = null;
            if (generator.nextInt(10) < RANDOM_THRESHOLD)
               operand2 = new ConstantExpression(value);
            else
               operand2 = new ConstantExpression(Value.generateRandomValue(generator, type));

            // Select an operator from the enumeration of valid operators
            Operator operator = null;
            do {
               operator = operators[generator.nextInt(operators.length)];
            } while (!isValidOperator(operator, type));
            
            // We now have all the components of a binary expression
            BinaryExpression expression = new BinaryExpression(operand1, operator, operand2);
            
            // We can now create a decision list node
            DecisionListExpression node = new DecisionListExpression(expression, generator.nextBoolean()); 

            // Add the new node to the end of the list
            if (lastNode != null)
               lastNode.nextNode = node;
            lastNode = node;
            
            // If this is the first node, remember it
            if (firstNode == null)
               firstNode = node;
         }
         
         // Add a default node to the end of the decision list
         Expression defaultExpression = new ConstantExpression(new Value(true));
         boolean defaultValue = generator.nextBoolean();
         lastNode.nextNode = new DecisionListExpression(defaultExpression, defaultValue);

         // Add the decision list to the list of expressions
         expressions.add(firstNode);
      }
      
      return expressions;
   }

   private static boolean isValidOperator(Operator operator, DataType type) {
      switch (type) {
         case BOOLEAN:
            if (operator == Operator.GREATER_THAN || operator == Operator.GREATER_THAN_OR_EQUAL_TO ||
                operator == Operator.LESS_THAN || operator == Operator.LESS_THAN_OR_EQUAL_TO)
                return false;
            return true;
         case FLOAT:
            return true;
         case INT:
            return true;
         case STRING:
            if (operator == Operator.EQUAL_TO || operator == Operator.NOT_EQUAL_TO)
               return true;
            return false;
         default:
            return true;
      }
   }
}
