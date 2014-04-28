package org.jiminy.cuda;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;

/**
 * This was just a "baby" kernel I whipped up to use as a reference while 
 * I coded the real thing.
 */
public class AddKernel extends KernelEngine {
   private int numElements = 32;
   private CUdeviceptr deviceInput1 = null;
   private CUdeviceptr deviceInput2 = null;
   private CUdeviceptr deviceOutput = null;
   
   public Pointer setupKernelParams() {
      float[] hostInput1 = new float[numElements];
      float[] hostInput2 = new float[numElements];
      for (int i = 0; i < numElements; i++) {
         hostInput1[i] = i;
         hostInput2[i] = i;
      }

      // Initialize device memory
      deviceInput1 = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(deviceInput1, numElements * Float.SIZE);
      JCudaDriver.cuMemcpyHtoD(deviceInput1, Pointer.to(hostInput1), numElements * Sizeof.FLOAT);
      
      deviceInput2 = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(deviceInput2, numElements * Float.SIZE);
      JCudaDriver.cuMemcpyHtoD(deviceInput2, Pointer.to(hostInput2), numElements * Sizeof.FLOAT);

      deviceOutput = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(deviceOutput, numElements * Sizeof.FLOAT);
      //JCudaDriver.cuMemsetD32(deviceOutput, 0, numElements);
      
      // Wrap values in "pointers"
      Pointer kernelParams = Pointer.to(
            Pointer.to(new int[]{numElements}),
            Pointer.to(deviceInput1),
            Pointer.to(deviceInput1),
            Pointer.to(deviceOutput));

      return kernelParams;
   }
   
   public void getResults() {
      float[] hostOutput = new float[numElements];
      JCudaDriver.cuMemcpyDtoH(Pointer.to(hostOutput), deviceOutput, numElements * Sizeof.FLOAT);
   }
   
   public void cleanup() {
      JCudaDriver.cuMemFree(deviceInput1);
      JCudaDriver.cuMemFree(deviceInput2);
      JCudaDriver.cuMemFree(deviceOutput);
   }

   @Override
   public int getNumBlocks() {
      return 16;
   }

   @Override
   public int getThreadsPerBlock() {
      return 512;
   }

   @Override
   public String getKernelSourceFileName() {
      return "addKernel.cu";
   }

   @Override
   public String getKernelFunctionName() {
      return "add";
   }
}
