package org.jiminy.cuda;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import jcuda.Pointer;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.CUstream;
import jcuda.driver.JCudaDriver;

public abstract class KernelEngine {
   public abstract Pointer setupKernelParams();
   public abstract void getResults();
   public abstract void cleanup();
   public abstract int getNumBlocks();
   public abstract int getThreadsPerBlock();
   public abstract String getKernelSourceFileName();
   public abstract String getKernelFunctionName();
   
   public KernelEngine() {}

   /**
    * Executes a call to the GPU
    */
   public void executeKernel() throws IOException {

      //////////////////////////////////////////////////////////////
      // Initialize

      JCudaDriver.setExceptionsEnabled(true);
      JCudaDriver.cuInit(0);
      
      String ptxFileName = compileCudaFile(getKernelSourceFileName(), true);
      
      // Get the device count
      int[] count = new int[1];
      JCudaDriver.cuDeviceGetCount(count);
     
      // Determine the optimal device
      // TO DO
      
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
      JCudaDriver.cuModuleGetFunction(kernelFunction, module, getKernelFunctionName());      

      //////////////////////////////////////////////////////////////
      // Set up the parameters to the kernel function

      // Set up kernel-specific parameters
      Pointer kernelParams = setupKernelParams();
      
      // Set up other kernel function parameters not specific to the function being called.
      // Yeah, I know: they're not really that independent of the kernel, but we'll pretend
      // in order to get a degree of abstraction.
      int gridDimX = getNumBlocks();
      int gridDimY = 1;
      int gridDimZ = 1;
      int blockDimX = getThreadsPerBlock();
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
      
      getResults();
      
      //////////////////////////////////////////////////////////////
      // Cleanup
      
      cleanup();

      JCudaDriver.cuCtxDestroy(ctx);
   }

   /**
    * Compiles a CU file to PTX 
    */
   private static String compileCudaFile(String cuFileName, boolean forceRebuild) throws IOException
   {
       int endIndex = cuFileName.lastIndexOf('.');
       if (endIndex == -1)
           endIndex = cuFileName.length()-1;

       String ptxFileName = cuFileName.substring(0, endIndex+1) + "ptx";
       
       File cuFile = new File(cuFileName);
       if (!cuFile.exists())
           throw new IOException("Input file not found: " + cuFileName);
       File ptxFile = new File(ptxFileName);
       if (ptxFile.exists() && ptxFile.lastModified() > cuFile.lastModified())
          return ptxFileName;

       String modelString = "-m" + System.getProperty("sun.arch.data.model");        
       String command = "nvcc " + modelString + " -ptx " + cuFile.getPath() + " -o " + ptxFileName + " -arch=sm_21";
       
       System.out.println("Compiling CU file to PTX: " + command);
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
