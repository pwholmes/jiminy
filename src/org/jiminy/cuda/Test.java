package org.jiminy.cuda;

import java.util.ArrayList;
import java.util.List;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.JCudaDriver;

public class Test {
   private static CUfunction multiplePointersKernel = null;
   
   public static List<String> processMultiplePointers(List<String> wordList)
   {
       int numWords = wordList.size();

       // Allocate and fill arrays on the device:
       // - One one for each input word, which is filled
       //   with the byte data for the respective word
       // - One for each output word
       CUdeviceptr dWordInputPointers[] = new CUdeviceptr[numWords];
       CUdeviceptr dWordOutputPointers[] = new CUdeviceptr[numWords];
       int wordLengths[] = new int[numWords];
       for(int i = 0; i < numWords; i++) {
          String word = wordList.get(i);
          byte hostWordData[] = word.getBytes();
          wordLengths[i] = hostWordData.length;
         
          dWordInputPointers[i] = new CUdeviceptr();
          JCudaDriver.cuMemAlloc(dWordInputPointers[i], wordLengths[i] * Sizeof.BYTE);
          JCudaDriver.cuMemcpyHtoD(dWordInputPointers[i], Pointer.to(hostWordData), wordLengths[i] * Sizeof.BYTE);
         
          dWordOutputPointers[i] = new CUdeviceptr();
          JCudaDriver.cuMemAlloc(dWordOutputPointers[i], wordLengths[i] * Sizeof.BYTE);
      }
     
      // Allocate device memory for the array of pointers
      // that point to the individual input words, and copy
      // the input word pointers from the host to the device.
      CUdeviceptr dWordInputPointersArray = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(dWordInputPointersArray, numWords * Sizeof.POINTER);
      JCudaDriver.cuMemcpyHtoD(dWordInputPointersArray, Pointer.to(dWordInputPointers), numWords * Sizeof.POINTER);
     
      // Allocate device memory for the array of pointers
      // that point to the individual output words, and copy
      // the output word pointers from the host to the device.
      CUdeviceptr dWordOutputPointersArray = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(dWordOutputPointersArray, numWords * Sizeof.POINTER);
      JCudaDriver.cuMemcpyHtoD(dWordOutputPointersArray, Pointer.to(dWordOutputPointers), numWords * Sizeof.POINTER);
     
      // Allocate and fill the device array for the word lengths
      CUdeviceptr dWordLengths = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(dWordLengths, numWords * Sizeof.INT);
      JCudaDriver.cuMemcpyHtoD(dWordLengths, Pointer.to(wordLengths), numWords * Sizeof.INT);
     
      // Set up the kernel parameters
      Pointer kernelParams = Pointer.to(
          Pointer.to(new int[]{numWords}),
          Pointer.to(dWordInputPointersArray),
          Pointer.to(dWordLengths),
          Pointer.to(dWordOutputPointersArray)
      );
     
      // Call the kernel function.
      int blockDimX = 256;
      int gridDimX = (int)Math.ceil((double)numWords/blockDimX);
      JCudaDriver.cuLaunchKernel(multiplePointersKernel,
          gridDimX, 1, 1,    // Grid dimension
          blockDimX, 1, 1,   // Block dimension
          0, null,           // Shared memory size and stream
          kernelParams, null // Kernel- and extra parameters
      );
      JCudaDriver.cuCtxSynchronize();

      // Copy the contents of each output pointer of the
      // device back into a host array, create a string
      // from each array and store it in the result list
      List<String> result = new ArrayList<String>();
      for(int i = 0; i < numWords; i++) {
          byte hostWordData[] = new byte[wordLengths[i]];
          JCudaDriver.cuMemcpyDtoH(Pointer.to(hostWordData), dWordOutputPointers[i], wordLengths[i] * Sizeof.BYTE);
          String word = new String(hostWordData);
          result.add(word);
      }

      // Clean up.
      for (int i = 0; i < numWords; i++) {
         JCudaDriver.cuMemFree(dWordInputPointers[i]);
         JCudaDriver.cuMemFree(dWordOutputPointers[i]);
      }
      JCudaDriver.cuMemFree(dWordInputPointersArray);
      JCudaDriver.cuMemFree(dWordOutputPointersArray);
      JCudaDriver.cuMemFree(dWordLengths);
     
      return result;
  }
}
