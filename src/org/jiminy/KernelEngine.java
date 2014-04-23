package org.jiminy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.CUstream;
import jcuda.driver.JCudaDriver;

public class KernelEngine {
   public void executeKernel() throws IOException {

      String kernelSourceFileName = "addKernel.cu";
      String kernelFunctionName = "add";
      
      //////////////////////////////////////////////////////////////
      // Initialize

      JCudaDriver.setExceptionsEnabled(true);
      JCudaDriver.cuInit(0);
      
      String ptxFileName = preparePtxFile(kernelSourceFileName);
      
      // Get the device count
      int[] count = new int[1];
      JCudaDriver.cuDeviceGetCount(count);
     
      // Determine the optimal device 
      System.out.println("device count: " + count[0]);
      
      // Select the chosen device
      CUdevice device = new CUdevice();
      JCudaDriver.cuDeviceGet(device, 0);

      // Get a device context
      CUcontext ctx = new CUcontext();
      JCudaDriver.cuCtxCreate(ctx, 0, device); 
      
      // Load the kernel
      CUmodule module = new CUmodule();
      JCudaDriver.cuModuleLoad(module, ptxFileName);
      
      // Get the kernel function
      CUfunction kernelFunction = new CUfunction();
      JCudaDriver.cuModuleGetFunction(kernelFunction, module, kernelFunctionName);      

      //////////////////////////////////////////////////////////////
      // Set up the parameters to the kernel function
      
      int numElements = 32;
      float[] hostInput1 = new float[numElements];
      float[] hostInput2 = new float[numElements];
      float[] hostOutput = new float[numElements];
      for (int i = 0; i < numElements; i++) {
         hostInput1[i] = i;
         hostInput2[i] = i;
      }

      // Initialize device memory
      CUdeviceptr deviceInput1 = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(deviceInput1, numElements * Float.SIZE);
      JCudaDriver.cuMemcpyHtoD(deviceInput1, Pointer.to(hostInput1), numElements * Sizeof.FLOAT);
      
      CUdeviceptr deviceInput2 = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(deviceInput2, numElements * Float.SIZE);
      JCudaDriver.cuMemcpyHtoD(deviceInput2, Pointer.to(hostInput2), numElements * Sizeof.FLOAT);

      CUdeviceptr deviceOutput = new CUdeviceptr();
      JCudaDriver.cuMemAlloc(deviceOutput, numElements * Sizeof.FLOAT);
      //JCudaDriver.cuMemsetD32(deviceOutput, 0, numElements);
      
      // Wrap values in "pointers"
      Pointer kernelParams = Pointer.to(
            Pointer.to(new int[]{numElements}),
            Pointer.to(deviceInput1),
            Pointer.to(deviceInput1),
            Pointer.to(deviceOutput));

      // Set up other kernel function parameters not specific to the function being called
      int gridDimX = 16;
      int gridDimY = 1;
      int gridDimZ = 1;
      int blockDimX = 512;
      int blockDimY = 1;
      int blockDimZ = 1;
      int sharedMemBytes = 0;
      CUstream hStream = null;
      Pointer extraParams = null;
      
      //////////////////////////////////////////////////////////////
      // Launch the kernel function

      JCudaDriver.cuLaunchKernel(
            kernelFunction,                       // kernel function
            gridDimX, gridDimY, gridDimZ,         // grid dimensions
            blockDimX, blockDimY, blockDimZ,      // block dimensions 
            sharedMemBytes,                       // shared memory size
            hStream,                              // stream
            kernelParams,                         // kernel parameters 
            extraParams);                         // "extra" parameters
      JCudaDriver.cuCtxSynchronize();
      
      //////////////////////////////////////////////////////////////
      // Get results from device
      
      JCudaDriver.cuMemcpyDtoH(Pointer.to(hostOutput), deviceOutput, numElements * Sizeof.FLOAT);
      
      //////////////////////////////////////////////////////////////
      // Cleanup
      
      JCudaDriver.cuMemFree(deviceInput1);
      JCudaDriver.cuMemFree(deviceInput2);
      JCudaDriver.cuMemFree(deviceOutput);
      JCudaDriver.cuCtxDestroy(ctx);
   }

   /**
    * The extension of the given file name is replaced with "ptx".
    * If the file with the resulting name does not exist, it is 
    * compiled from the given file using NVCC. The name of the 
    * PTX file is returned. 
    * 
    * @param cuFileName The name of the .CU file
    * @return The name of the PTX file
    * @throws IOException If an I/O error occurs
    */
   private static String preparePtxFile(String cuFileName) throws IOException
   {
       int endIndex = cuFileName.lastIndexOf('.');
       if (endIndex == -1)
           endIndex = cuFileName.length()-1;

       String ptxFileName = cuFileName.substring(0, endIndex+1)+"ptx";
       File ptxFile = new File(ptxFileName);
       if (ptxFile.exists())
           return ptxFileName;
       
       File cuFile = new File(cuFileName);
       if (!cuFile.exists())
           throw new IOException("Input file not found: "+cuFileName);

       String modelString = "-m" + System.getProperty("sun.arch.data.model");        
       String command = "nvcc " + modelString + " -ptx " + cuFile.getPath() + " -o " + ptxFileName;
       
       System.out.println("Executing\n" + command);
       Process process = Runtime.getRuntime().exec(command);

       String errorMessage = new String(toByteArray(process.getErrorStream()));
       String outputMessage = new String(toByteArray(process.getInputStream()));
       int exitValue = 0;
       try {
           exitValue = process.waitFor();
       }
       catch (InterruptedException e) {
           Thread.currentThread().interrupt();
           throw new IOException(
               "Interrupted while waiting for nvcc output", e);
       }

       if (exitValue != 0) {
           System.out.println("nvcc process exitValue " + exitValue);
           System.out.println("errorMessage:\n" + errorMessage);
           System.out.println("outputMessage:\n" + outputMessage);
           throw new IOException(
               "Could not create .ptx file: "+errorMessage);
       }
       
       System.out.println("Finished creating PTX file");
       return ptxFileName;
   }

   /**
    * Fully reads the given InputStream and returns it as a byte array
    *  
    * @param inputStream The input stream to read
    * @return The byte array containing the data from the input stream
    * @throws IOException If an I/O error occurs
    */
   private static byte[] toByteArray(InputStream inputStream) throws IOException {
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       byte buffer[] = new byte[8192];
       while (true) {
           int read = inputStream.read(buffer);
           if (read == -1)
               break;
           baos.write(buffer, 0, read);
       }
       return baos.toByteArray();
   }
}
