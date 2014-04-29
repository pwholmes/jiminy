package org.jiminy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.jiminy.cuda.DecisionListKernel;
import org.jiminy.domain.Value;
import org.jiminy.domain.expression.DecisionListExpression;

public class JiminyProgram {
   static JiminyProgram instance = null;
   private HashMap<String,Value> symbolTable = null;
   private ArrayList<DecisionListExpression> decisionLists = null;
   private final int numVariables = 1;
   private final int numDecisionLists = 1;
   private final int maxDLComplexity = 1;
   
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
      System.out.println("Parameters: variables: " + numVariables + "; expressions: " + numDecisionLists + "; max expression complexity: " + maxDLComplexity);
      System.out.println("Generating data...");

      symbolTable = DataGenerator.generateSymbolTable(numVariables);
      decisionLists = DataGenerator.generateDecisionLists(symbolTable, numDecisionLists, maxDLComplexity);

      System.out.println("Executing kernel...");
      
      DecisionListKernel kernel = new DecisionListKernel(symbolTable, decisionLists);
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
