typedef enum {DT_UNDEFINED = 0, DT_INT, DT_FLOAT, DT_STRING, DT_BOOLEAN} DataType;

typedef enum {OP_UNDEFINED = 0, OP_EQUAL_TO, OP_GREATER_THAN, OP_GREATER_THAN_OR_EQUAL_TO, OP_LESS_THAN, 
    OP_LESS_THAN_OR_EQUAL_TO, OP_LOGICAL_AND, OP_LOGICAL_OR, OP_NOT_EQUAL_TO} Operator;

typedef enum {DL_ERROR = 0, DL_FALSE = 1, DL_TRUE = 2, DL_IGNORE = 3} DLNodeValue;

typedef struct {
    DataType type;
    int intValue;
    float floatValue;
    char *stringValue;
    int booleanValue;
} Value;

#define OFFSET_SAFETY_MAX 100

__device__ int parseDecisionListNode(char *expression, DLNodeValue *value);
__device__ int parseExpression(char *expression, Value *value);
__device__ int parseBinaryExpression(char *expression, Value *value);
__device__ int parseVariableExpression(char *expression, Value *value);
__device__ int parseBooleanConstant(char *expression, Value *value);
__device__ int parseIntegerConstant(char *expression, Value *value);
__device__ int parseFloatConstant(char *expression, Value *value);
__device__ int parseStringConstant(char *expression, Value *value);
__device__ int parseOperator(char *expression, Operator *op);
__device__ void evaluateBinaryExpression(Value *operand1, Operator op, Value *operand2, Value *returnValue);
__device__ void evaluateIntegerComparison(int op1, Operator op, int op2, Value *value);
__device__ void evaluateFloatComparison(float op1, Operator op, float op2, Value *value);
__device__ void evaluateStringComparison(char *op1, Operator op, char *op2, Value *value);
__device__ void evaluateBooleanComparison(int op1, Operator op, int op2, Value *value);
__device__ int dstrlen(char *str);
__device__ int dstreql(char *str1, char *str2);

extern "C"
__global__ void processDecisionLists(int numExpressions, char **expressions, int *output)
{
    const unsigned int idx = blockIdx.x * blockDim.x + threadIdx.x;
    
    if (idx < numExpressions) {
        char *expression = expressions[idx];
        DLNodeValue dlNodeValue;
        int offset = parseDecisionListNode(expression, &dlNodeValue);
        output[idx] = dlNodeValue;
    }
}

/////////////////////////////////////////////////////////////////////
// PARSING FUNCTIONS
//
// NB: All the parseXXX functions return a value that indicates how far the pointer 
// should be advanced.  The actual return value is in the parameter list.

__device__ int parseDecisionListNode(char *expression, DLNodeValue *dlNodeValue) {
    
    // Currently there are only two valid formats for a DL node:
    //      <binary expression> <T|F>
    //      <boolean constant> <T|F>
    // In the latter case, the <boolean constant> must always be T since that represents
    // the default node.  It's redundant to have a condition that always evaluates to true,
    // but we keep it anyway because the code to generate, store and evaluate DL's on the 
    // Java side is much nicer that way.

    int offset = 0;
    Value value;
    
    offset += parseExpression(expression, &value);

    // Check the return from the expression evaluation.  If it's false, then we ignore this
    // DL node and move on to the next one (so return IGNORE); if true, then we return the
    // node's value.
    if (value.type != DT_BOOLEAN) {
        *dlNodeValue = DL_ERROR;
        return 0;
    }
    if (value.booleanValue == 0) {
        *dlNodeValue = DL_IGNORE; 
     } else {
        char nodeValue = *(expression+offset);
        if (nodeValue == 'T')
            *dlNodeValue = DL_TRUE;
        else if (nodeValue == 'F')
            *dlNodeValue = DL_FALSE;
        else {
            *dlNodeValue = DL_ERROR;
            return 0;
        }
     } 
    
    return offset;
}

__device__ int parseExpression(char *expression, Value *value) {
    int offset = 0;

    char c1 = expression[0];
    char c2 = expression[1];
    offset += 2;
    
    // NB: This is where you'd plug in the code to evaluate additional kinds of expressions
    // if you wanted to expand this kernel to be more generic.
    
    if (c1 == 'E' && c2 == 'B')
        offset += parseBinaryExpression(expression+offset, value);
    else if (c1 == 'E' && c2 == 'V')
        offset += parseVariableExpression(expression+offset, value);
    else if (c1 == 'C' && c2 == 'B')
        offset += parseBooleanConstant(expression+offset, value);
    else if (c1 == 'C' && c2 == 'I')
        offset += parseIntegerConstant(expression+offset, value);
    else if (c1 == 'C' && c2 == 'F')
        offset += parseFloatConstant(expression+offset, value);
    else if (c1 == 'C' && c2 == 'S')
        offset += parseStringConstant(expression+offset, value);
    else { // error
        value->type = DT_UNDEFINED;
        return 0;
    }

    return offset;
}

__device__ int parseBinaryExpression(char *expression, Value *value) {
    int offset = 0;

    // Skip over opening {
    if (*expression != '{')
        return 0;
    offset++;

    Value operand1;
    Operator op;
    Value operand2;
    offset += parseExpression(expression+offset, &operand1);
    offset += parseOperator(expression+offset, &op);
    offset += parseExpression(expression+offset, &operand2);

    // Evaluate the binary expression
    evaluateBinaryExpression(&operand1, op, &operand2, value);

    // Skip over closing }    
    if (*(expression+offset) != '}') {
        value->type = DT_UNDEFINED;
        return 0;
    }    
    offset++;

    return offset;
}

__device__ int parseVariableExpression(char *expression, Value *value) {
    int offset = 0;

    // Skip over opening {
    if (*expression != '{')
        return 0;
    offset++;

    char *token = expression+offset;
    while (*(expression+offset) != '}' && offset < OFFSET_SAFETY_MAX)
        offset++;
    if (offset == OFFSET_SAFETY_MAX)
        return 0;
    *(expression+offset) = '\0';
    offset++;

   
    // TODO: Look up variable in symbol table.
    // Of course, to do that we need to *have* a symbol table, so that's first on the list.


    return offset;
}

__device__ int parseBooleanConstant(char *expression, Value *value) {
    int offset = 0;

    // Skip over opening {
    if (*expression != '{')
        return 0;
    offset++;

    if (*(expression+offset) == 'F') {
        value->booleanValue = 0;
        value->type = DT_BOOLEAN;
    } else if (*(expression+offset) == 'T') {
        value->booleanValue = 1;
        value->type = DT_BOOLEAN;
    } else { // error
        value->type = DT_UNDEFINED;
        return 0; 
    }
    offset++;
    
    // Skip over closing }    
    if (*(expression+offset) != '}')
        return 0;
    offset++;

    return offset;
}

__device__ int parseIntegerConstant(char *expression, Value *value) {
    int offset = 0;

    // Skip over opening {
    if (*expression != '{')
        return 0;
    offset++;

    value->intValue = 0;
    while (*(expression+offset) != '}' && offset < OFFSET_SAFETY_MAX) {
        value->intValue = value->intValue * 10 + (*(expression+offset) - '0');  
        offset++;
    }
    if (offset == OFFSET_SAFETY_MAX)
        return 0;
    value->type = DT_INT;
    offset++;

    return offset;
}

__device__ int parseFloatConstant(char *expression, Value *value) {
    int offset = 0;

    // Skip over opening {
    if (expression[0] != '{')
        return 0;
    offset++;

    if (*(expression+offset) != '0')
        return 0;
    offset++;
    if (*(expression+offset) != '.')
        return 0;
    offset++;
    value->floatValue = 0;
    int divisor = 10;
    while (*(expression+offset) != '}' && offset < OFFSET_SAFETY_MAX) {
        value->floatValue = value->floatValue + ((float)(*(expression+offset) - '0'))/divisor; 
        divisor = divisor * 10;
        offset++;
    }
    if (offset == OFFSET_SAFETY_MAX)
        return 0;
    value->type = DT_FLOAT;
    offset++;

    return offset;
}

__device__ int parseStringConstant(char *expression, Value *value) {
    int offset = 0;

    // Skip over opening {
    if (*expression != '{')
        return 0;
    offset++;

    char *token = expression+offset;
    while (*(expression+offset) != '}' && offset < OFFSET_SAFETY_MAX)
        offset++;
    if (offset == OFFSET_SAFETY_MAX)
        return 0;
    *(expression+offset) = '\0';
    offset++;
    
    value->type = DT_STRING;
    value->stringValue = token; 

    return offset;
}

__device__ int parseOperator(char *expression, Operator *op) {
    char c1 = expression[0];
    char c2 = expression[1];
    
    if (c1 == '=' && c2 == '=')
        *op = OP_EQUAL_TO;
    else if (c1 == '>' && c2 == '>')
        *op = OP_GREATER_THAN;
    else if (c1 == '>' && c2 == '=')
        *op = OP_GREATER_THAN_OR_EQUAL_TO;
    else if (c1 == '<' && c2 == '<')
        *op = OP_LESS_THAN;
    else if (c1 == '<' && c2 == '=')
        *op = OP_LESS_THAN_OR_EQUAL_TO;
    else if (c1 == '&' && c2 == '&')
        *op = OP_LOGICAL_AND;
    else if (c1 == '|' && c2 == '|')
        *op = OP_LOGICAL_OR;
    else if (c1 == '!' && c2 == '=')
        *op = OP_NOT_EQUAL_TO;
    else // error
        return 0;
    
    return 2;
}

/////////////////////////////////////////////////////////////////////
// EVALUATION FUNCTIONS

__device__ void evaluateBinaryExpression(Value *operand1, Operator op, Value *operand2, Value *value) {
    // Indicate an error by not setting the type on the return value
    value->type = DT_UNDEFINED;
    
    // For now only allowing comparison of the same types
    if (operand1->type != operand2->type)
        return;
    switch (operand1->type) {
        case DT_INT:
            evaluateIntegerComparison(operand1->intValue, op, operand2->intValue, value);
            break;
        case DT_FLOAT:
            evaluateFloatComparison(operand1->floatValue, op, operand2->floatValue, value);
            break;
        case DT_STRING:
            evaluateStringComparison(operand1->stringValue, op, operand2->stringValue, value);
            break;
        case DT_BOOLEAN:
            evaluateBooleanComparison(operand1->booleanValue, op, operand2->booleanValue, value);
            break;
        default:
        case DT_UNDEFINED:
            // do nothing
            break;
    }
}

__device__ void evaluateIntegerComparison(int op1, Operator op, int op2, Value *value) {
    value->type = DT_BOOLEAN;
    int bv = 0;  // assume comparison is false
    switch (op) {
        case OP_EQUAL_TO:
            if (op1 == op2) bv = 1;
            break;
        case OP_GREATER_THAN:
            if (op1 > op2) bv = 1;
            break;
        case OP_GREATER_THAN_OR_EQUAL_TO:
            if (op1 >= op2) bv = 1;
            break;
        case OP_LESS_THAN:
            if (op1 < op2) bv = 1;
            break;
        case OP_LESS_THAN_OR_EQUAL_TO:
            if (op1 <= op2) bv = 1;
            break;
        case OP_LOGICAL_AND:
            bv = op1 && op2;
            break;
        case OP_LOGICAL_OR:
            bv = op1 || op2;
            break;
        case OP_NOT_EQUAL_TO:
            if (op1 != op2) bv = 1;
            break;
        default:
        case OP_UNDEFINED:
            break;
    }
    value->booleanValue = bv;
}

__device__ void evaluateFloatComparison(float op1, Operator op, float op2, Value *value) {
    value->type = DT_BOOLEAN;
    int bv = 0;  // assume comparison is false
    switch (op) {
        case OP_EQUAL_TO:
            if (op1 == op2) bv = 1;
            break;
        case OP_GREATER_THAN:
            if (op1 > op2) bv = 1;
            break;
        case OP_GREATER_THAN_OR_EQUAL_TO:
            if (op1 >= op2) bv = 1;
            break;
        case OP_LESS_THAN:
            if (op1 < op2) bv = 1;
            break;
        case OP_LESS_THAN_OR_EQUAL_TO:
            if (op1 <= op2) bv = 1;
            break;
        case OP_LOGICAL_AND:
            bv = op1 && op2;
            break;
        case OP_LOGICAL_OR:
            bv = op1 || op2;
            break;
        case OP_NOT_EQUAL_TO:
            if (op1 != op2) bv = 1;
            break;
        default:
        case OP_UNDEFINED:
            return;
    }
    value->booleanValue = bv;
}

__device__ void evaluateStringComparison(char *op1, Operator op, char *op2, Value *value) {
    // Because time is short, we'll have to skimp on the string comparisons
    // The greater than and less than operations require a lexical comparison,
    // and we don't have access to the standard C library (and thus strcmp()).
    // I'm not not going to write my own strcmp() function, so equality is the
    // only operation we're going to support for now.
      
    value->type = DT_BOOLEAN;
    int bv = 0;
    switch (op) {
        case OP_EQUAL_TO:
            if (dstreql(op1, op2) == 1) bv = 1;
            break;
        case OP_NOT_EQUAL_TO:
            if (dstreql(op1, op2) == 0) bv = 1;
            break;
        default:
        case OP_GREATER_THAN:
        case OP_GREATER_THAN_OR_EQUAL_TO:
        case OP_LESS_THAN:
        case OP_LESS_THAN_OR_EQUAL_TO:
        case OP_LOGICAL_AND:
        case OP_LOGICAL_OR:
        case OP_UNDEFINED:
            break;
    }
    value->booleanValue = bv;
}

__device__ void evaluateBooleanComparison(int op1, Operator op, int op2, Value *value) {
    value->type = DT_BOOLEAN;
    int bv = 0;
    switch (op) {
        case OP_EQUAL_TO:
            if (op1 == op2) bv = 1;
            break;
        case OP_LOGICAL_AND:
            bv = op1 && op2;
            break;
        case OP_LOGICAL_OR:
            bv = op1 || op2;
            break;
        case OP_NOT_EQUAL_TO:
            if (op1 != op2) bv = 1;
            break;
        default:
        case OP_GREATER_THAN:
        case OP_GREATER_THAN_OR_EQUAL_TO:
        case OP_LESS_THAN:
        case OP_LESS_THAN_OR_EQUAL_TO:
        case OP_UNDEFINED:
            break;
    }
    value->booleanValue = bv;
}

/////////////////////////////////////////////////////////////////////
// STRING FUNCTIONS

__device__ int dstrlen(char *str) {
    int len = 0;
    while (*str != '\0') {
        str++;
        len++;
    }
    return len;        
}

__device__ int dstreql(char *str1, char *str2) {
    while (*str1 == *str2 && *str1 != '\0' && *str2 != '\0') {
        str1++;
        str2++;
    }
    if (*str1 == '\0' && *str2 == '\0')
        return 1;
    return 0;
}
