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
   private final int numVariables = 100;
   private final int numDecisionLists = 1000;
   private final int maxDLLength = 3;
   
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
      System.out.println("Parameters: variables: " + numVariables + "; decision lists: " + numDecisionLists + "; max DL length: " + maxDLLength);
      System.out.println("Generating data...");

      symbolTable = DataGenerator.generateSymbolTable(numVariables);
      decisionLists = DataGenerator.generateDecisionLists(symbolTable, numDecisionLists, maxDLLength);

      System.out.println("Executing kernel...");
      long start = System.nanoTime();
      
      DecisionListKernel kernel = new DecisionListKernel(symbolTable, decisionLists);
      kernel.executeKernel();
      
      long stop = System.nanoTime();
      System.out.println("Kernel execution complete, time elapsed = " + (stop - start)/1000000 + " ms");

      System.out.println("Executing on host...");
      start = System.nanoTime();
      
      for (DecisionListExpression dl : decisionLists)
         dl.evaluate();
      
      stop = System.nanoTime();
      System.out.println("Host execution complete, time elapsed = " + (stop - start)/1000000 + " ms");
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
