package org.jiminy.domain;

public enum DecisionListResult {
   ERROR, FALSE, TRUE, IGNORE;
   
   public static DecisionListResult get(int numVal) {
      if (numVal == 1)
         return FALSE;
      else if (numVal == 2)
         return TRUE;
      else if (numVal == 3)
         return IGNORE;
      else
         return ERROR;
   }
}
