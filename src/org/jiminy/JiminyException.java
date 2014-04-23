package org.jiminy;

public class JiminyException extends Error {
   private static final long serialVersionUID = 1L;

   public JiminyException() {
      super();
   }

   public JiminyException(String message) {
      super(message);
   }

   public JiminyException(Throwable cause) {
      super(cause);
   }

   public JiminyException(String message, Throwable cause) {
      super(message, cause);
   }

}
