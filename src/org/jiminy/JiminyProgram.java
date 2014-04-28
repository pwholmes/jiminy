package org.jiminy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.jiminy.cuda.DecisionListKernel;
import org.jiminy.domain.Value;
import org.jiminy.domain.expression.Expression;

public class JiminyProgram {
   static JiminyProgram instance = null;
   private HashMap<String,Value> symbolTable = null;
   private ArrayList<Expression> expressions = null;
   private final int numVariables = 1;
   private final int numExpressions = 1;
   private final int maxExpressionComplexity = 1;
   
   private JiminyProgram() {}

   public static JiminyProgram createInstance() {
      instance = new JiminyProgram();
      return instance;
   }
   
   public static JiminyProgram getInstance() {
      if (instance == null)
         createInstance();
      return instance;
   }
   
   public HashMap<String,Value> getSymbolTable() {
      return symbolTable;
   }
   
   public void execute() throws JiminyException, IOException {
      System.out.println("Parameters: variables: " + numVariables + "; expressions: " + numExpressions + "; max expression complexity: " + maxExpressionComplexity);
      System.out.println("Generating data...");

      symbolTable = DataGenerator.generateSymbolTable(numVariables);
      expressions = DataGenerator.generateExpressions(symbolTable, numExpressions, maxExpressionComplexity);

      System.out.println("Exxecuting kernel...");
      
      DecisionListKernel kernel = new DecisionListKernel(symbolTable, expressions);
      kernel.executeKernel();
      
      System.out.println("Program complete.");
   }
   
   public static void main(String args[]) {
      try {
         JiminyProgram program = JiminyProgram.createInstance();
         program.execute();
      } catch (Throwable e) {
         System.out.println("Error executing program: " + e.getMessage());
         for (StackTraceElement elem: e.getStackTrace())
            System.out.println("  " + elem.toString());
      }
   }

}
