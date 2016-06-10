/**
 * ARMEngine -- define the generator for arm
 *
 * @author G. Dupont
 * @version 0.1
 */
package mcs.gc;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.time.LocalDateTime;
import mcs.symtab.*;
import mcs.compiler.*;
import mcs.obj.*;

public class ARMEngine extends AbstractMachine {
	/**
	 * Number of registers.
	 * ARM has in fact 15 "multi-purpose registers", but three of them is used for stack, link and program counter.
	 * Plus, we decided to use R12 as the heap top and R11 as the stack base, as they are not implemented directly into ARM.
   * Moreover, we use R10 as a register for storing the object's class id when we jump from methods to vtables.
	 */
	static private final int NUM_REGISTER = 10;
	static private final String Prefix = "\t\t", Spacing = "\t\t";  // For a nice code
	private List<Register> registers;							// List of registers on the machine
	private Register sp, lr, pc, ht, sb, oi;	// Special registers
	private int heapbase = 0;											// Manual heap base calculus
  private int condition_nb = 0;									// Number of if-then-else (/other conditionnal) structures

	/**
	 * Constructor
	 */
	public ARMEngine() {
		// This mainly build registers
		registers = new ArrayList<Register>();

		for (int i = 0; i < NUM_REGISTER; i++) {
			registers.add(new Register("r", i));
		}
		sp = new Register("sp", -1);
		lr = new Register("lr", -1);
		pc = new Register("pc", -1);
		ht = new Register("r", 12, "HT");
		sb = new Register("r", 11, "SB");
    oi = new Register("r", 10, "OI");
	}

	/**
	 * Suffix for target file : asm
	 */
	public String getSuffix() {
		return "s";
	}

	/**
	 * Write the given code to a file.
	 * This will : 1) Build the introduction to the file 2) Write the code to the file
	 * @param fileName name of the file
	 * @param code code to write
	 */
	@Override
	public void writeCode(String fileName, String code) throws MCSException {
		// Generate the vtables
    String vtables =
      generateAllVtables() +
      "\n" +
      generateComment("@@", "");

		// Generate the init
		String init =
			generateComment("Initialize registers", "") +
			generateInstruction("MOV", ht, "HB") +
			generateInstruction("MOV", sb, sp) +
			"\n";

		// Generate preliminary
    String preliminary = 
			// Header
      generateMultiComments(
          "@@\n" +
          "File generated by microC# compilator for ARM (v7)\n" +
          "On: " + LocalDateTime.now() +  "\n" +
          "@@\n",
          "") +
      "\n" +
			// Declarations
      generateComment("Preliminary definitions : heap top, stack base, heap base", "") +
			ht.alias() + " EQU " + ht.name() + "\n" +
			sb.alias() + " EQU " + sb.name() + "\n" +
      oi.alias() + " EQU " + oi.name() + "\n" +
			"HB EQU " + (heapbase+5)*4 + "\n" +
			"\n";

		// Actually write the code to the file
		super.writeCode(fileName, preliminary + init + vtables + code);
	}

	/**
	 * Generate every virtual tables recorded so far.
	 * @return the code
	 */
  private String generateAllVtables() throws MCSException {
    String code = generateMultiComments(
        "@@\n" +
        "Code for virtual tables\n" +
        "@@\n", "");

    for (MethodInfo mi : VirtualTableCentral.instance().vtables().keySet()) {
      code += generateVtable(mi);
    }

    return code;
  }

	/**
	 * Generate the code for a virtual table
	 */
  public String generateVtable(MethodInfo mi) throws MCSException {
		// Retrieve the vtable for this method
		VirtualTable vt = VirtualTableCentral.instance().vtables().get(mi);

		// Generate a little header
    String code =
      generateComment("Virtual Table for method " + mi.label(), "") +
      generateLabel(mi.label() + ".vtable");

		// Generate the code for each entry
    for (int key : vt.allKeys()) {
      code +=
				generateInstruction("CMP", oi, key) +
				generateInstruction("BEQ", vt.get(key) + mi.label());
    }

    return code + "\n";
  }

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
  public String generateLoadValue(VariableInfo info, Register rout) throws MCSException  {
    String code = "";

    if (info instanceof ConstantInfo)
      code += generateLoadConstant((ConstantInfo)info, rout);
    else {
      code += generateLoadFromStack(info.displacement(), rout);
    }

    return code;
  }

  /**
   * Generate the code for loading a variable into a register, with an optionnal field name (for structs)
   * @param info info of the variable to load
   * @param disp (integer) displacement to consider (for structs)
   * @param rout (out) register in which the value will be
   * @return the generated code
   */
  public String generateLoadValue(VariableInfo info, int disp, Register rout) throws MCSException  {
    String code = "";
    Register r = getNextUnusedRegister();
    Register raddr = new Register();
		Object odisp;

    if (info.type() instanceof CompositeType) {
      code += generateLoadFromStack(info.displacement(), raddr);

      if (disp < 65536) {
				odisp = disp;
      } else {
        Register rdisp = new Register();
        code += generateLoadConstant(new ConstantInfo(new IntegerType(), disp), rdisp);
        odisp = rdisp;
        rdisp.setStatus(Register.Status.Used);
      }

      raddr.setStatus(Register.Status.Used);
    } else {
      // we shouldn't be using this function, don't we ?
      return "";
    }

    code +=
			generateInstruction("LDR", true, r, raddr, odisp);

    r.setStatus(Register.Status.Loaded);
    rout.copy(r);

    return code;
  }

  /**
   * Generate the code for loading a variable into a register, with an optionnal displacement register (for arrays)
   * @param info info of the variable to load
   * @param rdisp (register) displacement to consider
   * @param rout (out) register in which the value will be
   * @return the generated code
   */
  public String generateLoadValue(VariableInfo info, Register rdisp, Register rout) throws MCSException  {
    Register raddr = new Register();
    Register r = getNextUnusedRegister();
    String code = "";
    Type t = info.type();

    if (t instanceof CompositeType) {
      code +=
				generateLoadFromStack(info.displacement(), raddr) +
				generateInstruction("LDR", true, r, raddr, rdisp);
    } else {
      // not supposed to be called, isn't it ?
      return "";
    }

    raddr.setStatus(Register.Status.Used);
    rdisp.setStatus(Register.Status.Used);
    r.setStatus(Register.Status.Loaded);
    rout.copy(r);

    return code;
  }

  /**
   * Generate the code for loading a value from the stack to a register
   * @param disp displacement of the variable to load
   * @param rout (out) register in which the value will be
   * @return the generated code
   */
  public String generateLoadFromStack(int disp, Register rout) throws MCSException  {
    Register r = getNextUnusedRegister();
    String code = "";
		Object snd;

    if (disp < 65536) {
      snd = -disp;
    } else {
      Register rval = new Register();
      code += generateLoadConstant(new ConstantInfo(new IntegerType(), -disp), rval);
      snd = rval;
      rval.setStatus(Register.Status.Used);
    }
    code +=
			generateInstruction("LDR", true, r, sb, snd);

    r.setStatus(Register.Status.Loaded);
    rout.copy(r);

    return code;
  }

  /**
	 * Generate the code for loading a constant itneger into a register.
	 * @param info info of the constant to load
	 * @param rout register where the value is put, for later referencing
	 * @return the generated code
	 */
	public String generateLoadConstant(ConstantInfo info, Register rout) throws MCSException  {
    String code = "";
    Register r = getNextUnusedRegister();
    Type t = info.type();

    if (t instanceof SimpleType) {
      Object o = info.value();
			System.out.println("gLC : " + o.getClass());

			int val = (Integer)o;

      code +=
				generateInstruction("MOV", r, (val & 0x0000FFFF));

      if (val >= 65536) {
        code +=
					generateInstruction("MOVT", r, (val >> 16));
      }
    } else if (t instanceof StructType) {
      // TODO
    } else if (t instanceof ArrayType) {
      // TODO
    } else if (t instanceof Klass) {
    }

    r.setStatus(Register.Status.Loaded);
    rout.copy(r);

    return code;
  }

  /**
   * Generate the code for loading a variable from the heap into a register
   * @param raddr register containing the address
   * @param disp optionnal displacement (integer) for struct
   * @param rout register where the value is put
   * @return the generated code
   */
  public String generateLoadFromHeap(Register raddr, int disp, Register rout) throws MCSException  {
    Register r = new Register();
    String code = "";

    if (disp < 65536) {
      r = getNextUnusedRegister();
      code += 
				generateInstruction("LDR", true, r, raddr, disp);
      r.setStatus(Register.Status.Loaded);
      raddr.setStatus(Register.Status.Used);
      rout.copy(r);
    } else {
      code += generateLoadConstant(new ConstantInfo(new IntegerType(), disp), r);
      code += generateLoadFromHeap(raddr, r, rout);
    }

    return code;
  }

  /**
   * Generate the code for loading a variable from the heap into a register, with an optionnal register displacement
   * @param raddr register containing the address
   * @param rdisp (register) optionnal displacement in a register (for arrays)
   * @param rout register where the value is put
   * @return the generated code
   */
  public String generateLoadFromHeap(Register raddr, Register rdisp, Register rout) throws MCSException  {
    String code = "";
    Register r = getNextUnusedRegister();

    code +=
			generateInstruction("LDR", true, r, raddr, rdisp);

    raddr.setStatus(Register.Status.Used);
    rdisp.setStatus(Register.Status.Used);
    r.setStatus(Register.Status.Loaded);
    rout.copy(r);

    return code;
  }

  ///////////// STORE ////////////

  /**
   * Generate the code for 'updating' the value of a variable in the stack
   * @param info info of the variable to store
   * @param rin value to put in the variabe
   * @return the generated code
   */
  public String generateStoreVariable(VariableInfo vinfo, Register rin) throws MCSException  {
    Type t = vinfo.type();
    String code = "";

    if (t instanceof SimpleType) {
      code +=
				generateInstruction("STR", true, rin, sb, -vinfo.displacement());
    } else if (t instanceof StructType) {
      // Shouldn't be called like that
    } else if (t instanceof ArrayType) {
      // Shouldn't be called like that
    } else if (t instanceof Klass) {
    }

    rin.setStatus(Register.Status.Used);
    return code;
  }

  /**
   * Generate the code for 'updating' the value of a variable in the stack, with an additionnal displacement
   * @param info info of the variable to store
   * @param disp (integer) displacement to consider
   * @param rin value to put in the variabe
   * @return the generated code
   */
  public String generateStoreVariable(VariableInfo vinfo, int disp, Register rin) throws MCSException  {
    String code = "", addr = "";
    Type t = vinfo.type();

    if (t instanceof CompositeType) {
      Register raddr = new Register();
      code +=
        generateLoadFromStack(vinfo.displacement(), raddr) +
        generateStoreInHeap(raddr, disp, rin);
    } else {
      // Shouldn't be called ?
    }

    return code;
  }

  /**
   * Generate the code for 'updating' the value of a variable in the stack
   * @param info info of the variable to store
   * @param rdisp (register) displacement to consider
   * @param rin value to put in the variabe
   * @return the generated code
   */
  public String generateStoreVariable(VariableInfo vinfo, Register rdisp, Register rin) throws MCSException  {
    String code = "";
    Type t = vinfo.type();

    if (t instanceof CompositeType) {
      Register raddr = new Register();
      code +=
        generateLoadFromStack(vinfo.displacement(), raddr) +
        generateStoreInHeap(raddr, rdisp, rin);
    } else {
      // Shouldn't be called ?
    }

    return code;
  }

  /**
   * Generate the code for storing a variable into the heap
   * @param raddr register containing the address
   * @param disp (integer) optionnal displacement for structs
   * @param rin register containing the value to be stored
   * @return the generated code
   */
  public String generateStoreInHeap(Register raddr, int disp, Register rin) throws MCSException  {
    String code = "", addr = raddr + ", ";

    raddr.setStatus(Register.Status.Used);

    if (disp < 65536) {
			code +=
				generateInstruction("STR", true, rin, raddr, disp);
    } else {
      Register r = new Register();
      code +=
        generateLoadConstant(new ConstantInfo(new IntegerType(), disp), r) +
				generateInstruction("STR", true, rin, raddr, r);
      r.setStatus(Register.Status.Used);
    }
    
    rin.setStatus(Register.Status.Used);

    return code;
  }

  /**
   * Generate the code for storing a variable into the heap, with optionnal register displacement
   * @param raddr register containing the address
   * @param rdisp (register) optionnal displacement for arrays
   * @param rin register containing the value to be stored
   * @return the generated code
   */
  public String generateStoreInHeap(Register raddr, Register rdisp, Register rin) throws MCSException  {
    String code =
			generateInstruction("STR", true, rin, raddr, rdisp);
    raddr.setStatus(Register.Status.Used);
    rdisp.setStatus(Register.Status.Used);
    rin.setStatus(Register.Status.Used);
    return code;
  }

  /**
   * Generate the code for allocating a variable in the stack
   * @param type type to allocate
   * @return the generated code
   */
  public String generateAllocateInStack(Type type) throws MCSException  {
    String code = "";

    if (type instanceof CompositeType) {
      Register raddr = new Register();
      code +=
				generateAllocate(type, raddr, null) +
				generateInstruction("PUSH", raddr);
    } else {
			code +=
				generateInstruction("ADD", sp, sp, type.size());
    }

    return code;
  }
	
  /**
	 * Generate the code for allocating a block in the heap
	 * @param type type to allocate
	 * @param raddr (out) register containing the address of the block
	 * @param rsize register containing the size of the block (array only)
	 * @return the generated code
	 */
	public String generateAllocate(Type type, Register raddr, Register rsize) throws MCSException  {
		Register reg = getNextUnusedRegister();

		String code =
			generateInstruction("MOV", reg, ht);

		if (type instanceof StructType) {
			StructType ts = (StructType)type;
			Register r = new Register();
			for (Type t : ts.fieldsTypes()) {
				code += generateAllocate(t, r, null);
			}
		} else if (type instanceof Klass) {
      Klass k = (Klass)type;
      Register r = new Register();
      for (Type t : k.attributeTypes()) {
        code += generateAllocate(t, r, null);
      }
    } else if (type instanceof ArrayType) {
			ArrayType t = (ArrayType)type;
			Register rs = getNextUnusedRegister();
			code +=
				generateInstruction("UMUL", rs, rsize, t.getType().size()) +
				generateInstruction("ST", true, rs, ht) +
				generateInstruction("ADD", ht, ht, 4) +
				generateInstruction("ADD", ht, ht, rs);
			rsize.setStatus(Register.Status.Used);
		} else {
			Register rs = new Register();
			code +=
				generateLoadConstant(new ConstantInfo(new IntegerType(), type.size()), rs) +
				generateInstruction("ADD", ht, ht, rs);
		}

		reg.setStatus(Register.Status.Loaded);
		raddr.copy(reg);

		return code;
	}

	/**
	 * Generate the code for flushing the stack top variable
	 * @param type type of the variable
	 * @return the generated code
	 */
	public String generateFlushVariable(Type type) throws MCSException  {
		Register reg = getNextUnusedRegister();

		return 
			generateInstruction("POP", reg);
	}

	/**
	 * Generate the code for flushing every variable of a symbol table
	 * Note: used when going out from a block
	 * @param symtab the symbol table
	 * @return the generated code
	 */
	public String generateFlush(SymbolTable symtab) throws MCSException  {
		Register reg = getNextUnusedRegister();
		String code = "";
		ListIterator<Type> iter = symtab.symbolsTypes().listIterator(symtab.symbols().size());

		while (iter.hasPrevious()) {
			code += generateFlushVariable(iter.previous());
		}

		return code;
	}


	/// Functions related

	/**
	 * Generate the code for the beginning of declaring a function
	 * @param info the info of the function
	 * @param code code generated for the content of the function
	 * @return the generated code
	 */
	public String generateFunctionDeclaration(FunctionInfo info, String blockcode) throws MCSException {
		Register r = getNextUnusedRegister();
		info.assignRegister(r);

    String label = info.label();

    if (info instanceof MethodInfo)
      label = ((MethodInfo)info).parent().name() + label;
		
		String code =
			generateMultiComments(
					"@@\n" +
					" Function: " + info.toString() + "\n" +
					"@@\n", "") +
			generateLabel(label) +
			generateComment("Push link register, stack base and stack pointer", ARMEngine.Prefix) +
			generateInstruction("PUSH", lr) +
			generateInstruction("PUSH", sb) +
			generateInstruction("PUSH", sp) +
			"\n" +
			generateComment("Body", ARMEngine.Prefix) +
			blockcode +
			"\n";

		// End of the function
		if (!(info.returnType() instanceof VoidType)) {
			code +=
				generateComment("Default return. It is not wise to reach this point", ARMEngine.Prefix) +
				generateInstruction("MOV", info.register(), ht) +
				generateInstruction("ADD", ht, ht, 4) +
				"\n";
		}

		code +=
			generateLabel(label + "_end") +
			generateComment("Pop registers", ARMEngine.Prefix) +
			generateInstruction("POP", sp) +
			generateInstruction("POP", sb) +
			generateInstruction("POP", lr) +
			"\n" +
			generateComment("Pop arguments", ARMEngine.Prefix);

		// As we push arguments in one order, we need to pop them in the
		// other
    if (info instanceof MethodInfo) { // It's a method ! We must first pop the object
      code +=
        generateFlushVariable(new IntegerType());
    }

		ListIterator<Type> iter = info.parameters().listIterator(info.parameters().size());
		while (iter.hasPrevious()) {
			code += generateFlushVariable(iter.previous());
		}

		code +=
      generateComment("Jump back to preceding context", ARMEngine.Prefix) +
			generateInstruction("BX", lr) + "\n";

		return code;
	}


	/**
	 * Generate the code for the 'return' keyword
	 * @param info the info of the fuunction
	 * @param rval the register containing the value to be returned
	 * @return the generated code
	 */
	public String generateFunctionReturn(FunctionInfo info, Register rval) throws MCSException {
		String code = "", label = info.label();

    if (info instanceof MethodInfo)
      label = ((MethodInfo)info).parent().name() + label;

		if (!(info.returnType() instanceof VoidType)) {
			Register r = getNextUnusedRegister();
			info.assignRegister(r);
			code +=
				generateInstruction("MOV", info.register(), ht) +
				generateInstruction("STMIA", "!" + ht, rval);

			info.register().setStatus(Register.Status.Loaded);
			rval.setStatus(Register.Status.Used);
		}

		code +=
			generateInstruction("B", label + "_end") + "\n";

		return code;
	}

	/**
	 * Generate the code for pushing an argument
	 * @param reg register in which the argument is stored
	 * @return the generated code
	 */
	public String generateFunctionPushArgument(Register reg) throws MCSException  {
		String code =
			generateInstruction("PUSH", reg);
		reg.setStatus(Register.Status.Used);
		return code;
	}


	/**
	 * Generate the code for the call to a function
	 * @param info info of the function
	 * @return the generated code
	 */
	public String generateFunctionCall(FunctionInfo info) throws MCSException  {
		String code =
			generateInstruction("BL", info.label());
		return code;
	}

  /**
   * Generate the code for the declaration of a method
   * @param info info of the method
   * @param blockcode code of the content of the method
   * @return the generated code
   */
  public String generateMethodDeclaration(MethodInfo info, String blockcode) throws MCSException {
    Klass kmeth = info.parent();
    String code =
      generateComment("Vtable redirection", ARMEngine.Prefix) +
      // We need to retrieve the object's id. First, we get the address of the object,
      // which is just below the context, so with a displacement of -16
			generateInstruction("LDR", true, oi, sb, -16) +
      // Then we load the very first field of the object (displacement 0)
			generateInstruction("LDR", true, oi, oi) +
      // We then compare the id of the retrieved object to the id of the class
			generateInstruction("CMP", oi, kmeth.classId()) +
      // Then we branch to the vtable if needed
			generateInstruction("BEQ", info.label() + ".vtable") +
      "\n" +
      // Next part is the "real code" that we labellize with .body
      generateLabel(kmeth.name() + info.label()) +
    	blockcode;

    return generateFunctionDeclaration(info, code);
  }

  /**
   * Generate the code for the call of a method
   * @param info info of the method
   * @param robj register containing the address of the object on which we call the method
   * @return the generated code
   */
  public String generateMethodCall(MethodInfo info, Register robj) throws MCSException {
    String code =
      generateFunctionPushArgument(robj) +
      generateFunctionCall(info);
    return code;
  }

  /**
   * Generate the code for declaring a constructor
   * @param info info of the constructor (the register attribute) 
   * @param bcode code of the block
   * @return the generated code
   */
  public String generateConstructorDeclaration(ConstructorInfo info, String bcode) throws MCSException {
    Register r = getNextUnusedRegister();

    String codeinst =
      generateLabel(info.parent().name() + info.label() + "_inst") +
      generateComment("Instanciate the class", ARMEngine.Prefix) +
			generateInstruction("MOV", r, ht);

    Klass k = info.parent();
    int rs = k.realSize();

    if (rs < 65535) {
      codeinst +=
				generateInstruction("ADD", ht, ht, rs);
    } else {
      Register rsize = new Register();
      codeinst +=
        generateLoadConstant(new ConstantInfo(new IntegerType(), rs), rsize) +
				generateInstruction("ADD", ht, ht, rsize);
    }

    codeinst +=
			generateInstruction("PUSH", r);

    info.assignRegister(r);
    r.setStatus(Register.Status.Loaded);

    return codeinst + generateFunctionDeclaration(info, bcode);
  }

  /**
   * Generate the code for calling a constructor
   * @param info info of the constructor to call
	 * @param base determines if this call is made to the super constructor (in another constructor)
   * @return the generated code
   */
  public String generateConstructorCall(ConstructorInfo info, boolean base) throws MCSException {
    String code =
			generateInstruction("BL", info.parent().name() + info.label() + (base ? "" : "_inst"));
    return code;
  }
  
	////////////////////////////// MISC ///////////////////////////////
	/**
	 * Generate an instruction with various number of parameters
	 * @param inst instruction
	 * @param p1,p2,... parameters
	 * @param enclose enclose the last parameters in brackets (for addressing)
	 * @return the generated code
	 */
	public String generateInstruction(String inst, boolean enclose, Object p1, Object p2, Object p3) throws MCSException {
		heapbase++;
		String code =
			ARMEngine.Prefix + inst;

		if (p1 != null)
			code += ARMEngine.Spacing + format(p1);

		if (p2 != null)
			code += "," + ARMEngine.Spacing +  (enclose ? "[" : "") + format(p2);

		if (p3 != null)
			code += "," + (enclose ? " " : ARMEngine.Spacing) + format(p3);

		if (p2 != null && enclose)
			code += "]";

		return code + "\n";
	}

	public String generateInstruction(String inst, Object p1, Object p2, Object p3) throws MCSException {
		return generateInstruction(inst, false, p1, p2, p3);
	}

	public String generateInstruction(String inst, boolean enclose, Object p1, Object p2) throws MCSException {
		return generateInstruction(inst, enclose, p1, p2, null);
	}

	public String generateInstruction(String inst, Object p1, Object p2) throws MCSException {
		return generateInstruction(inst, false, p1, p2);
	}

	public String generateInstruction(String inst, Object p1) throws MCSException {
		return generateInstruction(inst, p1, null);
	}
	
	/**
	 * Generate a label
	 * @param label
	 * @return the generated code
	 */
	public String generateLabel(String label) throws MCSException {
		return label + ":\n";
	}

	/**
	 * Generate a direct constant for use in instructions
	 * @param val value of the constant
	 * @return the generated code
	 */
	public String generateDirect(int value) throws MCSException {
		return "$" + Integer.toString(value);
	}
	
	/**
	 * Generate a register
	 * @param reg the register
	 * @return the generated code
	 */
	public String generateRegister(Register reg) throws MCSException {
		return "%" + reg.toString();
	}

	private String format(Object o) throws MCSException {
		if (o instanceof String)
			return (String)o;
		else if (o instanceof Integer)
			return generateDirect((Integer)o);
		else if (o instanceof Register)
			return generateRegister((Register)o);
		
		throw new MCSWrongUseException("format()");
	}

  /**
   * Generate a comment
   * @param com comment to print
   * @param indent indent to apply
   * @return the generated code
   */
  public String generateComment(String com, String indent) throws MCSException {
    if (com.equals("@@"))
      return indent + "////////////////////////////////////////////////////////////////////\n";
    else
      return indent + "// " + com + "\n";
  }

  /**
   * Generate a multiline comment (utility)
   * @param com list of '\n' separated comments
   * @param indent indent to apply
   * @return the generated code
   */
  public String generateMultiComments(String com, String indent) throws MCSException {
    String code = "";

    String[] lines = com.split("\n");
    for (String l : lines)
      code += generateComment(l, indent);

    return code;
  }


	/**
	 * Generate the code for making an address from a list of displacement pair.
	 * The principle is to get into a register the addres of, let us say, the field
	 * of a structure. The thing is that we can chain struct fields calls, thus
	 * making the address calculation quite complex.
	 * To achieve this calculation, we first create a displacement list, storing
	 * displacement of each fields one by one (as welle as a boolean indicating
	 * if should dereference the field (arrow) or not (point).
	 * Then, we can create the addres by a succession of LDR
	 * @param dlist displacement list
	 * @param raddr (out) register that will contain the address
	 * @return the generated code
	 */
	public String generateMakeAddress(DisplacementList dlist, Register raddr) throws MCSException {
		return generateMakeAddress(dlist, sb, raddr);
	}

	/**
	 * Generate the code for making an address from a list of displacement pair,
	 * using the specified register as base register.
	 * @param dlist displacement list
	 * @param rbaseaddr base address register
	 * @param raddr (out) register that will contain the address
	 * @return the generated code
	 */
	public String generateMakeAddress(DisplacementList dlist, Register rbaseaddr, Register raddr) throws MCSException {
    String code = "";
		Register r = getNextUnusedRegister();
		DisplacementPair dp;
		
		// First displacement is the one of the struct itself
		// it is special because it is relative to the stack
		ListIterator<DisplacementPair> iter = dlist.listIterator();
		dp = iter.next();
		code +=
			generateInstruction("LDR", true, r, rbaseaddr, -dp.disp);

		while (iter.hasNext()) {
			dp = iter.next();
			if (dp.deref) {
				code +=
					generateInstruction("LDR", true, r, r);
      }
			code +=
				generateInstruction("LDR", true, r, r, dp.disp);
		}

		rbaseaddr.setStatus(Register.Status.Used);
		r.setStatus(Register.Status.Loaded);
		raddr.copy(r);

		return code;
	}

  /**
   * Generate the code for an if-then-else structure
   * @param rcond register containing the result of the condition
   * @param cif code for the if branch
   * @param celse code for the else branch
   * @return the generated code
   */
  public String generateIfThenElse(Register rcond, String cif, String celse) throws MCSException {
		boolean else_present = !(celse.isEmpty());
    String code = 
			generateInstruction("CBZ", rcond, (else_present ? "else" : "end" + "_" + condition_nb)) +
      cif + "\n";

		if (else_present) {
			code +=
				generateInstruction("B", "end_" + condition_nb) +
      	generateLabel("else_" + condition_nb) +
      	celse + "\n";
		}

		code +=
			generateLabel("end_" + condition_nb) + "\n";

    rcond.setStatus(Register.Status.Used);
    condition_nb++;

    return code;
  }



	/// Calculus
  
  public String generateOperation(int op, Register r1, Register r2, Register rout) throws MCSException {
    Operator oop = IMachine.IntToOperator[op];
    if (oop == Operator.NOP)
      return "";
    else if (oop.isArithmetic())
      return generateArithOperation(oop, r1, r2, rout);
    else
      return generateRelOperation(oop, r1, r2, rout);
  }

  public String generateOperation(int op, Register r1, Register rout) throws MCSException {
    Operator oop = IMachine.IntToOperator[op];
    if (oop == Operator.NOP)
      return "";
    else if (oop.isArithmetic())
      return generateArithOperation(oop, r1, rout);
    else
      return generateRelOperation(oop, r1, rout);
  }

  /**
	 * Generate an arithmetic binary operation
	 * @param r1 first register
	 * @param r2 second register
	 * @param rout output register
	 * @return the generated code
	 */
	private String generateArithOperation(Operator op, Register r1, Register r2, Register rout) throws MCSException  {
		// TODO: wrong operation type
		// The last part of the code never changes : xxx Rx, R<1>, R<2>

		String code = "", instr = "";

		// Get the next register
		Register r = getNextUnusedRegister();

		switch (op) {
			case ADD:
				instr = "ADD";
				break;
			case SUB:
				instr = "SUB";
				break;
			case MUL:
				instr = "MUL";
				break;
			case DIV:
				instr = "DIV";
				break;
			case AND:
				instr = "AND";
				break;
			case OR:
				instr = "OR";
				break;
			case MOD:
				// nop
				break;
		}

		if (op != Operator.MOD) {
			code +=
				generateInstruction(instr, r, r1, r2);
		} else {	
			// Do this : q = a/b, b*q, a-bq = r
			code +=
				generateInstruction("SDIV", r, r1, r2) +
				generateInstruction("MUL", r, r2, r) +
				generateInstruction("SUB", r, r1, r);
		}

		// Source register are no longer used
		r1.setStatus(Register.Status.Used);
		r2.setStatus(Register.Status.Used);


		// Manage register
		r.setStatus(Register.Status.Loaded);
		rout.copy(r);

		// End
		return code;
	}

	/**
	 * Generate an arithmetic unary operation
	 * @param rin source register
	 * @param rout destination register
	 * @return the generated code
	 */
	private String generateArithOperation(Operator op, Register rin, Register rout) throws MCSException  {
		// Find the operation code
		String opcode = "", code = "";
		switch (op) {
			case NEG:
				opcode = "RSB";
				break;
			case NOT:
				opcode = "MVN";
				break;
			case PLS:
				break;
		}

		// Source register are no longer used
		rin.setStatus(Register.Status.Used);

		// Retrieve next register
		Register r = getNextUnusedRegister();

		// Generate code
		if (op != Operator.PLS) {
			code =
				generateInstruction(opcode, r, rin, 0);
		}

		// Manage register
		r.setStatus(Register.Status.Loaded);
		rout.copy(r);

		// End
		return code;
	}

	private String generateRelOperation(Operator op, Register r1, Register r2, Register rout) throws MCSException  {
		Register r = getNextUnusedRegister();
		String cc = "", operand = "";
		String code = 
			generateInstruction("MOV", r, 0);

		switch (op) {
			case EQ:
				cc = "EQ";
				operand = "CMP";
				break;
			case NEQ:
				cc = "NE";
				operand = "CMP";
				break;
			case LT:
				cc = "LT";
				operand = "CMP";
				break;
			case LEQ:
				cc = "LE";
				operand = "CMP";
				break;
			case GT:
				cc = "GT";
				operand = "CMP";
				break;
			case GEQ:
				cc = "GE";
				operand = "CMP";
				break;
			case RAND:
				operand = "ANDS";
				cc = "EQ";
				break;
			case ROR:
				operand = "ORRS";
				cc = "EQ";
				break;
		}

		code +=
			generateInstruction(operand, r1, r2) +
			generateInstruction("MOV" + cc, r, 1);

		// Information about register
		r1.setStatus(Register.Status.Used);
		r2.setStatus(Register.Status.Used);


		// Manage register
		r.setStatus(Register.Status.Loaded);
		rout.copy(r);

		return code;
	}

	/**
	 * Generate an relationnal binary operation
	 * @param rin input register 
	 * @param rout output register
	 * @return the generated code
	 */
	private String generateRelOperation(Operator op, Register rin, Register rout) throws MCSException {
		Register r = getNextUnusedRegister();
		String code =
			generateInstruction("MOV", r, 0);
		
		switch (op) {
			case RNOT:
				code +=
					generateInstruction("CMP", rin, 0) +
					generateInstruction("MOVNE", r, 1);
				break;
		}

		// Information about register
		rin.setStatus(Register.Status.Used);

		// Manage register
		r.setStatus(Register.Status.Loaded);
		rout.copy(r);

		return code;
	}


	/**************************************************/
	/**
	 * Get the next unused register in the register database
	 * @return the register
	 */
	private Register getNextUnusedRegister() throws MCSException {
		// TODO: register use policy
		for (int i = 0; i < registers.size(); i++) {
			if (registers.get(i).status() == Register.Status.Empty)
				return registers.get(i);
		}

		for (int i = 0; i < registers.size(); i++) {
			if (registers.get(i).status() == Register.Status.Used)
				return registers.get(i);
		}

    throw new MCSRegisterLimitReachedException();
	}

	public String logRegisters() {
		String txt = "";
		for (Register r : this.registers) {
			txt += r + " => ";
			switch (r.status()) {
				case Empty:
					txt += "E";
					break;
				case Loaded:
					txt += "L";
					break;
				case Used:
					txt += "U";
					break;
			}
			txt += "\n";
		}
		return txt;
	}
}


