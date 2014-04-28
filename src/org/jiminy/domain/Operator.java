package org.jiminy.domain;

import org.jiminy.JiminyException;

public enum Operator {
   EQUAL_TO, GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESS_THAN_OR_EQUAL_TO, LOGICAL_AND, LOGICAL_OR, NOT_EQUAL_TO;
   
   public static String encode(Operator op) {
      switch (op) {
         case EQUAL_TO:                   return "==";
         case GREATER_THAN:               return ">>";
         case GREATER_THAN_OR_EQUAL_TO:   return ">=";
         case LESS_THAN:                  return "<<";
         case LESS_THAN_OR_EQUAL_TO:      return "<=";
         case LOGICAL_AND:                return "&&";
         case LOGICAL_OR:                 return "||";
         case NOT_EQUAL_TO:               return "!=";
         default:
            throw new JiminyException("Unknown operator: " + op);
         
      }
   }
}
