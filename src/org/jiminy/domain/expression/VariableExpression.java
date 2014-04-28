package org.jiminy.domain.expression;

import java.util.HashMap;
import org.jiminy.JiminyException;
import org.jiminy.JiminyProgram;
import org.jiminy.domain.Value;

public class VariableExpression implements Expression {
   private String name;

   public VariableExpression(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   @Override
   public void setValue(Value value) {
      HashMap<String,Value> symbolTable = JiminyProgram.getInstance().getSymbolTable();
      if (symbolTable == null)
         throw new JiminyException("No symbol table!");
      value = symbolTable.get(name);
      if (value == null)
         throw new JiminyException("Undeclared variable: " + name);
      symbolTable.put(name,value);
   }
   
   @Override
   public Value getValue() {
      HashMap<String,Value> symbolTable = JiminyProgram.getInstance().getSymbolTable();
      if (symbolTable == null)
         throw new JiminyException("No symbol table!");
      Value value = symbolTable.get(name);
      if (value == null)
         throw new JiminyException("Undeclared variable: " + getName());
      return value;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      
      sb.append("{").append(this.getClass().getSimpleName()).append(": ");
      sb.append("name: ").append(getName());
      sb.append("; value: ").append(getValue());
      sb.append("}");
      
      return sb.toString();
   }

   // TODO: Need to write code to send the symbol table to the GPU, and look up variables in the symbol table,
   // before we can send variables to the GPU!  In the mean time, look up the variables on this end instead.
   @Override
   public String encode() {
      StringBuilder sb = new StringBuilder();
      //sb.append("EV{");
      //sb.append(name);
      //sb.append("}");

      sb.append("{");
      Value value = JiminyProgram.getInstance().getSymbolTable().get(name);
      switch(value.getType()) {
         case BOOLEAN:  sb.append("CB{"); break;
         case FLOAT:    sb.append("CF{"); break;
         case INT:      sb.append("CI{"); break;
         case STRING:   sb.append("CS{"); break;
         default:
      }
      sb.append(value.getStringValue());
      sb.append("}");
      
      return sb.toString();
   }

}
