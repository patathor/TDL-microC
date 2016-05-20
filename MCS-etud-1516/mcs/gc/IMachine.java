package mcs.gc;

import mcs.compiler.MCSException;
import mcs.symtab.VariableInfo;
import mcs.symtab.FunctionInfo;
import mcs.symtab.SymbolTable;
import mcs.symtab.ConstantInfo;
import mcs.symtab.Type;

/**
 * Cette interface décrit une machine cible. A compléter, selon votre modèle
 * 
 * @author marcel
 * 
 */
public interface IMachine {
	/**
	 * Enumeration that define possible arithmetic operations
	 */
	public enum Operator {
		ADD, // Addition
		SUB, // Substraction
		MUL, // Multiplication
		DIV, // Division
		NEG, // Arithmetic inversion (that is : minus)
    		NOP,  // Syntaxic stuff only
		AND, // And bitwise
		OR, // Or bitwise
    		MOD, // Modulo operator
    		PLS, // Unary plus
    		NOT // Not operator
	}
	
	public enum RelationalOperator {
		EQ,  // Equal
		NEQ, // Non equal
		LT,  // Lesser
		LEQ, // Strict Inferior
		GT,  // Superior
		GEQ, // Strict Superior
		AND, // And 
		OR,  // Or
		NOT, // Not
	}

	/**
	 * Suffixe du fichier cible (.tam par exemple)
	 * 
	 * @return
	 */
	String getSuffix();

	/**
	 * Ecrit le code dans un fichier à partir du nom du fichier source et du
	 * suffixe
	 * 
	 * @param fileName
	 * @param code
	 * @throws MCSException
	 */

	void writeCode(String fileName, String code) throws MCSException;

	/**********************************************************
	 * Generation function
	 **********************************************************/

	/////////////////////// MEMORY INSTRUCTIONS ///////////////////////
  
  ///////////// LOAD /////////////
 
  /**
   * Generate the code for loading a variable into a register
   * @param info info of the variable to load
   * @param rout (out) register in which the value will be
   * @return the generated code
   */
  String generateLoadValue(VariableInfo info, Register rout);

  /**
   * Generate the code for loading a variable into a register, with an optionnal field name (for structs)
   * @param info info of the variable to load
   * @param disp (integer) displacement to consider (for structs)
   * @param rout (out) register in which the value will be
   * @return the generated code
   */
  String generateLoadValue(VariableInfo info, int disp, Register rout);

  /**
   * Generate the code for loading a variable into a register, with an optionnal displacement register (for arrays)
   * @param info info of the variable to load
   * @param rdisp (register) displacement to consider
   * @param rout (out) register in which the value will be
   * @return the generated code
   */
  String generateLoadValue(VariableInfo info, Register rdisp, Register rout);

  /**
   * Generate the code for loading a value from the stack to a register
   * @param disp displacement of the variable to load
   * @param rout (out) register in which the value will be
   * @return the generated code
   */
  String generateLoadFromStack(int disp, Register rout);

  /**
	 * Generate the code for loading a constant itneger into a register.
	 * @param info info of the constant to load
	 * @param rout register where the value is put, for later referencing
	 * @return the generated code
	 */
	String generateLoadConstant(ConstantInfo info, Register rout);

  /**
   * Generate the code for loading a variable from the heap into a register
   * @param raddr register containing the address
   * @param disp optionnal displacement (integer) for struct
   * @param rout register where the value is put
   * @return the generated code
   */
  String generateLoadFromHeap(Register raddr, int disp, Register rout);

  /**
   * Generate the code for loading a variable from the heap into a register, with an optionnal register displacement
   * @param raddr register containing the address
   * @param rdisp (register) optionnal displacement in a register (for arrays)
   * @param rout register where the value is put
   * @return the generated code
   */
  String generateLoadFromHeap(Register raddr, Register rdisp, Register rout);

  ///////////// STORE ////////////

  /**
   * Generate the code for 'updating' the value of a variable
   * @param info info of the variable to store
   * @param rin value to put in the variabe
   * @return the generated code
   */
  String generateStoreVariable(VariableInfo vinfo, Register rin);

  /**
   * Generate the code for 'updating' the value of a variable
   * @param info info of the variable to store
   * @param disp (integer) displacement to consider
   * @param rin value to put in the variabe
   * @return the generated code
   */
  String generateStoreVariable(VariableInfo vinfo, int disp, Register rin);

  /**
   * Generate the code for 'updating' the value of a variable
   * @param info info of the variable to store
   * @param rdisp (register) displacement to consider
   * @param rin value to put in the variabe
   * @return the generated code
   */
  String generateStoreVariable(VariableInfo vinfo, Register rdisp, Register rin);

  /**
   * Generate the code for storing a variable into the heap
   * @param raddr register containing the address
   * @param disp (integer) optionnal displacement for structs
   * @param rin register containing the value to be stored
   * @return the generated code
   */
  String generateStoreInHeap(Register raddr, int disp, Register rin);

  /**
   * Generate the code for storing a variable into the heap, with optionnal register displacement
   * @param raddr register containing the address
   * @param rdisp (register) optionnal displacement for arrays
   * @param rin register containing the value to be stored
   * @return the generated code
   */
  String generateStoreInHeap(Register raddr, Register rdisp, Register rin);
  
  /////// MEMORY MANAGEMENT ///////
  /**
   * Generate the code for allocating a variable in the stack
   * @param type type to allocate
   * @return the generated code
   */
  String generateAllocateInStack(Type type);

  /**
   * Generate the code for allocating a block in the heap
   * @param type type to allocate
   * @param raddr (out) register containing the address of the block
   * @param rsize register containing the size of the block (array only)
   * @return the generated code
   */
  String generateAllocate(Type type, Register addr, Register rsize);

  /**
   * Generate the code for flushing the stack top variable
   * @param type type of the variable
   * @return the generated code
   */
  String generateFlushVariable(Type type);

	/**
	 * Generate the code for flushing every variable of a symbol table
	 * Note: used when going out from a block
	 * @param symtab the symbol table
	 * @return the generated code
	 */
	String generateFlush(SymbolTable symtab);

  /////////////////////// FUNCTION MANAGEMENT ///////////////////////

  /**
   * Generate the code for the beginning of declaring a function
   * @param info the info of the function
   * @param code code generated for the content of the function
   * @return the generated code
   */
  String generateFunctionDeclaration(FunctionInfo info, String code);

  /**
   * Generate the code for the 'return' keyword
   * @param info the info of the function
   * @param vinfo the info of the value to return
   * @return the generated code
   */
  String generateFunctionReturn(FunctionInfo info, VariableInfo vinfo);

  /**
   * Generate the code for pushing an argument
   * @param reg register in which the argument is stored
   * @return the generated code
   */
  String generateFunctionPushArgument(Register reg);

  /**
   * Generate the code for the call to a function
   * @param info info of the function
   * @return the generated code
   */
  String generateFunctionCall(FunctionInfo info);

	//////////////////////////// CALCULUS /////////////////////////////

	/**
	 * Generate an arithmetic binary operation
	 * @param r1 first register
	 * @param r2 second register
	 * @param rout output register
	 * @return the generated code
	 */
	String generateOperation(Operator op, Register r1, Register r2, Register rout);

	/**
	 * Generate an arithmetic unary operation
	 * @param rin source register
	 * @param rout destination register
	 * @return the generated code
	 */
	String generateOperation(Operator op, Register rin, Register rout);

	/**
	 * Generate a relational binary operation
	 * @param r1 first register
	 * @param r2 second register
	 * @param rout output register
	 * @return the generated code
	 */
	String generateOperation(RelationalOperator op, Register r1, Register r2, Register rout);

	/**
	 * Generate a relational unary operation
	 * @param rin source register
	 * @param rout destination register
	 * @return the generated code
	 */
	String generateOperation(RelationalOperator op, Register rin, Register rout);
}
