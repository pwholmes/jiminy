package org.jiminy;

public class JCudaTest
{
    public static void main(String args[]) {
       try {
          KernelEngine engine = new KernelEngine();
          engine.executeKernel();
       } catch (Throwable e) {
          System.out.println("Error executing kernel: " + e.getMessage());
       }
    }
}