package org.jiminy.domain;

import java.util.Random;
import org.jiminy.JiminyException;

public class Value {
   private DataType type;
   private int intValue = 0;
   private float floatValue = 0;
   private String stringValue = null;
   private boolean booleanValue = false;
   
   public Value(DataType type) {
      this.type = type;
   }
   
   public Value(int intValue) {
      this.type = DataType.INT;
      this.intValue = intValue;
   }

   public Value(float floatValue) {
      this.type = DataType.FLOAT;
      this.floatValue = floatValue;
   }
   
   public Value(String stringValue) {
      this.type = DataType.STRING;
      this.stringValue = stringValue;
   }

   public Value(boolean booleanValue) {
      this.type = DataType.BOOLEAN;
      this.booleanValue = booleanValue;
   }

   public DataType getType() {
      return this.type;
   }
   
   public int getIntValue() {
      if (type == DataType.INT)
         return intValue;
      else if (type == DataType.FLOAT)
         return (int)floatValue;
      else if (type == DataType.STRING) {
         try {
            return Integer.valueOf(stringValue);
         } catch (Throwable e) {
            throw new JiminyException("Cannot convert type " + getType() + " to type " + type + ".", e);
         }
      }
      else // (type == VariableType.BOOLEAN)
         throw new JiminyException("Cannot convert type " + getType() + " to type " + type + ".");
   }

   public float getFloatValue() {
      if (type == DataType.INT)
         return (float)intValue;
      else if (type == DataType.FLOAT)
         return floatValue;
      else if (type == DataType.STRING) {
         try {
            return Float.valueOf(stringValue);
         } catch (Throwable e) {
            throw new JiminyException("Cannot convert type " + getType() + " to type " + type + ".", e);
         }
      }
      else // (type == VariableType.BOOLEAN)
         throw new JiminyException("Cannot convert type " + getType() + " to type " + type + ".");
   }

   public String getStringValue() {
      if (type == DataType.INT)
         return String.valueOf(intValue);
      else if (type == DataType.FLOAT)
         return String.valueOf(floatValue);
      else if (type == DataType.STRING)
         return stringValue;
      else  { // (type == VariableType.BOOLEAN)
         return booleanValue ? "T" : "F";
      }
   }

   /**
    * For now I'm allowing Int and Float types to be implicitly converted into Booleans (if != 0 then true, otherwise false).
    * I'm  not sure that's a good idea, though, so keep an eye on this...
    */
   public boolean getBooleanValue() {
      if (type == DataType.INT)
         return (intValue != 0);
      else if (type == DataType.FLOAT)
         return (floatValue != 0);
      else if (type == DataType.STRING)
         return Boolean.valueOf(stringValue);
      else // (type == VariableType.BOOLEAN)
         return booleanValue;
   }

   public Value copy() {
      Value value = new Value(type);
      value.booleanValue = booleanValue;
      value.intValue = intValue;
      value.floatValue = floatValue;
      value.stringValue = stringValue;
      return value;
   }
   
   public boolean equals(Value value) {
      return Value.equals(this, value);
   }
   
   public static boolean equals(Value value1, Value value2) {
      if (value1 == null || value2 == null)
         return false; // equating anything to null is false
      
      if (value1.getType() != value2.getType())
         return false;
      
      switch (value1.getType()) {
         case BOOLEAN:
            return (value1.getBooleanValue() == value2.getBooleanValue());
         case FLOAT:
            return (value1.getFloatValue() == value2.getFloatValue());
         case INT:
            return (value1.getIntValue() == value2.getIntValue());
         case STRING:
            return (value1.getStringValue() == value2.getStringValue());
      }
      return false; // should never get here
   }
   
   public int compareTo(Value value) {
      return Value.compare(this, value);
   }
   
   /**
    * Returns 0 if the values are equal, a value < 0 if value 1 < value2, and a value > 0 if value1 > value2.
    * For booleans, true > false.
    */
   public static int compare(Value value1, Value value2) {
      if (value1 == null || value2 == null)
         throw new JiminyException("Cannot compare null values");
      
      if (value1.getType() != value2.getType())
         throw new JiminyException("Values being compared are not of the same type.");

      switch (value1.getType()) {
         case BOOLEAN:
            return Boolean.compare(value1.getBooleanValue(), value2.getBooleanValue());
         case FLOAT:
            return Float.compare(value1.getFloatValue(), value2.getFloatValue());
         case INT:
            return Integer.compare(value1.getIntValue(), value2.getIntValue());
         case STRING:
            return value1.getStringValue().compareTo(value2.getStringValue());
      }

      throw new JiminyException("Unknown data type: " + value1.getType()); // should never get here
   }
   
   public Value convertTo(DataType type) {
      switch (type) {
         case BOOLEAN:
            return new Value(getBooleanValue());
         case FLOAT:
            return new Value(getFloatValue());
         case INT:
            return new Value(getIntValue());
         case STRING:
            return new Value(getStringValue());
         default:
            throw new JiminyException("Unknown data type: " + type); // should never get here
      }
   }
   
   public static Value generateRandomValue(Random generator, DataType type) {
      switch (type) {
         case BOOLEAN:
            return new Value(generator.nextBoolean());
         case FLOAT:
            return new Value(generator.nextFloat());
         case INT:
            return new Value(generator.nextInt(10000));
         case STRING:
            return new Value("str" + (generator.nextInt(10000) + 1));
         default:
            throw new JiminyException("Unknown data type: " + type); // should never get here
      }
   }
   
   public String toString() {
      StringBuilder sb = new StringBuilder();
      
      sb.append("{").append(this.getClass().getSimpleName()).append(": ");
      sb.append("type: ").append(getType());
      sb.append("; intValue: ").append(intValue);
      sb.append("; floatValue: ").append(floatValue);
      sb.append("; stringValue: ").append(stringValue);
      sb.append("; booleanValue: ").append(booleanValue);
      sb.append("}");
      
      return sb.toString();
   }
}
