package org.jiminy.cuda;

import java.util.ArrayList;
import java.util.HashMap;
import org.jiminy.domain.Value;
import org.jiminy.domain.expression.DecisionListExpression;
import org.jiminy.domain.expression.Expression;
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
   private ArrayList<Expression> expressions;
   private int[] hOutput = null;
   //private CUdeviceptr dSymbolTable = null;
   private CUdeviceptr dExpressionPointers = null;
   private CUdeviceptr[] dExpressions = null;
   private CUdeviceptr dOutput = null;
   private int numExpressions = 0;
   
   public DecisionListKernel(HashMap<String,Value> symbolTable, ArrayList<Expression> expressions) {
      this.symbolTable = symbolTable;
      this.expressions = expressions;
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
      for (Expression e : expressions) {
         DecisionListExpression dl = (DecisionListExpression)e;
         encodedExpressions.addAll(dl.encode2());
      }
      
      numExpressions = encodedExpressions.size();
      
      System.out.println("Copying data to device...");

      // Allocate device memory for the string for each expression
      dExpressions = new CUdeviceptr[numExpressions];
      for (int i = 0; i < numExpressions; i++) {
         System.out.println(encodedExpressions.get(i));
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
      dOutput = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(dOutput, numExpressions * Sizeof.INT);
      
      // Wrap values in "Pointers"
      Pointer kernelParams = Pointer.to(
            Pointer.to(new int[]{numExpressions}),
            Pointer.to(dExpressionPointers),
            Pointer.to(dOutput));

      return kernelParams;
   }

   @Override
   public void getResults() {
      System.out.println("Copying results from device...");
      hOutput = new int[numExpressions];
      JCudaDriver.cuMemcpyDtoH(Pointer.to(hOutput), dOutput, numExpressions * Sizeof.INT);
      System.out.println("");
   }

   @Override
   public void cleanup() {
      for (int i = 0; i < numExpressions; i++)
         JCudaDriver.cuMemFree(dExpressions[i]);
      JCudaDriver.cuMemFree(dExpressionPointers);
   }
}
