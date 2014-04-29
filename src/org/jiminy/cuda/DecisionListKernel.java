package org.jiminy.cuda;

import java.util.ArrayList;
import java.util.HashMap;
import org.jiminy.JiminyException;
import org.jiminy.domain.DecisionListResult;
import org.jiminy.domain.Value;
import org.jiminy.domain.expression.DecisionListExpression;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;

/**
 * This is the Java side of the call to the kernel code on the GPU
 * that evaluates the decision lists.
 */
public class DecisionListKernel extends KernelEngine {
   private HashMap<String,Value> symbolTable;
   private ArrayList<DecisionListExpression> decisionLists;
   private ArrayList<Boolean> decisionListResults = new ArrayList<Boolean>();
   private CUdeviceptr dSymbolTable = null;
   private CUdeviceptr dExpressionPointers = null;
   private CUdeviceptr[] dExpressions = null;
   private CUdeviceptr dResults = null;
   private int numExpressions = 0;
   
   public DecisionListKernel(HashMap<String,Value> symbolTable, ArrayList<DecisionListExpression> decisionLists) {
      this.symbolTable = symbolTable;
      this.decisionLists = decisionLists;
   }
   
   @Override
   public int getNumBlocks() {
      return (int)Math.ceil((float)numExpressions/512);
   }

   @Override
   public int getThreadsPerBlock() {
      return 512;
   }

   @Override
   public String getKernelSourceFileName() {
      return "decisionListKernel.cu";
   }

   @Override
   public String getKernelFunctionName() {
      return "processDecisionLists";
   }

   @Override
   public Pointer setupKernelParams() {
      System.out.println("Encoding data...");
      
      // Encode the expressions for transmission to the GPU
      ArrayList<String> encodedExpressions = new ArrayList<String>();
      for (DecisionListExpression dl : decisionLists)
         encodedExpressions.addAll(dl.encode2());

      numExpressions = encodedExpressions.size();
      
      System.out.println("Copying data to device...");

      // Allocate device memory for the string for each expression
      dExpressions = new CUdeviceptr[numExpressions];
      for (int i = 0; i < numExpressions; i++) {
         byte[] expr = encodedExpressions.get(i).getBytes();
         dExpressions[i] = new CUdeviceptr();
         JCudaDriver.cuMemAlloc(dExpressions[i], (expr.length + 1) * Sizeof.BYTE);
         JCudaDriver.cuMemsetD8(dExpressions[i], (byte)0, (expr.length + 1));
         JCudaDriver.cuMemcpyHtoD(dExpressions[i], Pointer.to(expr), expr.length * Sizeof.BYTE);
      }
      
      // We also have to allocate device memory for the *pointers* to the strings
      dExpressionPointers = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(dExpressionPointers, numExpressions * Sizeof.POINTER);
      JCudaDriver.cuMemcpyHtoD(dExpressionPointers, Pointer.to(dExpressions), numExpressions * Sizeof.POINTER);

      // Allocate the output memory on the device
      dResults = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(dResults, numExpressions * Sizeof.INT);
      
      // Wrap values in "Pointers"
      Pointer kernelParams = Pointer.to(
            Pointer.to(new int[]{numExpressions}),
            Pointer.to(dExpressionPointers),
            Pointer.to(dResults));

      return kernelParams;
   }

   @Override
   public void getResults() {
      System.out.println("Copying results from device...");
      int[] hExpressionResults = new int[numExpressions];
      JCudaDriver.cuMemcpyDtoH(Pointer.to(hExpressionResults), dResults, numExpressions * Sizeof.INT);
      
      // When we sent the data to the GPU, we separated each decision list into its component
      // expressions and sent them separately.  Now we have to recombine the results into their
      // owning decision list.  This is easy because both the decision lists and results are ordered.
      int resultIndex = 0;
      for (DecisionListExpression dl : decisionLists) {
         DecisionListExpression node = dl;
         boolean gotResult = false;
         while (node != null) {
            DecisionListResult nodeResult = DecisionListResult.get(hExpressionResults[resultIndex++]);
            if (nodeResult == DecisionListResult.ERROR)
               throw new JiminyException("Error returned from kernel");
            else if (!gotResult && nodeResult == DecisionListResult.TRUE) {
               decisionListResults.add(true);
               gotResult = true;
            } else if (!gotResult && nodeResult == DecisionListResult.FALSE) {
               decisionListResults.add(false);
               gotResult = true;
            }  
            node = node.getNextNode();
         }
      }
   }

   @Override
   public void cleanup() {
      for (int i = 0; i < numExpressions; i++)
         JCudaDriver.cuMemFree(dExpressions[i]);
      JCudaDriver.cuMemFree(dExpressionPointers);
   }
}
