extern crate byteorder;

use std::error::Error;
use std::env;
use std::fs::File;
use std::io::{Read, ErrorKind};
use byteorder::{BigEndian, ReadBytesExt};
use std::iter::Peekable;
use std::str::Chars;
use std::collections::BTreeMap;

#[derive(Debug)]
enum Constant {
	Class(u16),
	Fieldref(u16, u16),
	Methodref(u16, u16),
	InterfaceMethodref(u16, u16),
	String(u16),
	Integer(i32),
	Float(f32),
	Long(i64),
	Double(f64),
	NameAndType(u16, u16),
	Utf8(String),
	MethodHandle,
	MethodType,
	InvokeDynamic,
	Unusable,
}

fn get_string_constant(constants: &Vec<Constant>, id: u16) -> &str {
	let constant = &constants[id as usize - 1];
	match *constant {
		Constant::Utf8(ref s) => s,
		_ => panic!("Constant is not a string: {:?}", constant),
	}
}

fn get_class_constant(constants: &Vec<Constant>, id: u16) -> &str {
	let constant = &constants[id as usize - 1];
	match *constant {
		Constant::Class(name_id) => get_string_constant(constants, name_id),
		_ => panic!("Constant is not a class: {:?}", constant),
	}
}

#[derive(Debug)]
struct Field {
	access_flags: u16,
	name_index: u16,
	descriptor: FieldType,
	signature: Option<String>,
	constant_value: Option<u16>,
}

#[derive(Debug)]
struct ExceptionTableEntry {
		start_pc: u16,
		end_pc: u16,
		handler_pc: u16,
		catch_type: u16,
}

#[derive(Debug,Clone)]
enum StackType {
	Boolean,
	Byte,
	Short,
	Character,
	Integer,
	Long,
	Float,
	Double,
	Reference,
}

impl Copy for StackType {}

#[derive(Debug,Clone)]
enum CompareType {
	EQ,
	NE,
	LT,
	GE,
	GT,
	LE,
}

#[derive(Debug,Clone)]
enum BinaryOperation {
	Add,
	Subtract,
	Multiply,
	Divide,
	Remainder,
	LeftShift,
	RightArithmeticShift,
	RightLogicalShift,
	BitwiseAnd,
	BitwiseOr,
	BitwiseXor,
}

#[derive(Debug,Clone)]
enum Instruction {
	Nop,
	IntegerConstant(i32),
	LongConstant(i64),
	FloatConstant(f32),
	DoubleConstant(f64),
	NullConstant,
	ArrayLoad(StackType, usize, usize),
	ArrayStore(StackType, usize, usize, usize),
	ReturnVoid,
	Return(StackType, usize),
	GetStatic(u16),
	BinaryOperation(StackType, BinaryOperation, usize, usize),
	Negate(StackType, usize),
	New(u16),
	Throw,
	Convert(StackType, StackType, usize),
	GetField(u16),
	PutField(u16),
	PutStatic(u16),
	InvokeVirtual(u16),
	InvokeSpecial(u16),
	InvokeStatic(u16),
	InvokeInterface(u16),
	Compare(StackType, usize, usize),
	GotoIf(CompareType, usize, usize, usize),
	Goto(usize),
	NewArray(u8),
	NewReferenceArray(u16),
	CheckCast(u16),
	InstanceOf(u16),
	MonitorEnter,
	MonitorExit,
	ArrayLength,
	MultiNewArray(u16, u8),
	Argument(u16),
}

#[derive(Debug, Clone)]
enum VerificationTypeInfo {
  Top,
  Integer,
  Float,
  Long,
  Double,
  Null,
  UninitializedThis,
  Object(String),
  Uninitialized,
}

impl VerificationTypeInfo {
	fn to_stack_type(&self) -> StackType {
		match *self {
			VerificationTypeInfo::Integer => StackType::Integer,
			VerificationTypeInfo::Float => StackType::Float,
			VerificationTypeInfo::Long => StackType::Long,
			VerificationTypeInfo::Double => StackType::Double,
			VerificationTypeInfo::Object(_) => StackType::Reference,
			_ => panic!("Invalid verification type: {:?}", self),
		}
	}
}

#[derive(Debug, Clone)]
enum FieldType {
	Byte,
	Character,
	Double,
	Float,
	Integer,
	Long,
	Reference(String),
	Short,
	Boolean,
	Array(Box<FieldType>),
}

#[derive(Debug)]
struct MethodType(Vec<FieldType>, Option<FieldType>);

#[derive(Debug)]
struct StackMapFrame {
	offset: u32,
	stack: Vec<VerificationTypeInfo>,
	locals: Vec<VerificationTypeInfo>,
}

#[derive(Debug)]
struct Code {
	max_stack: u16,
	max_locals: u16,
	instructions: BTreeMap<usize, Vec<Instruction>>,
	exception_table: Vec<ExceptionTableEntry>,
}

#[derive(Debug)]
struct Method {
	access_flags: u16,
	name_index: u16,
	exceptions: Vec<String>,
	descriptor: MethodType,
	signature: Option<String>,
	code: Option<Code>
}

#[derive(Debug)]
struct ClassFile {
	constant_pool: Vec<Constant>,
	access_flags: u16,
	this_class: u16,
	super_class: u16,
	interfaces: Vec<u16>,
	fields: Vec<Field>,
	methods: Vec<Method>,
	source_file: Option<u16>,
}

fn parse_error(message: &str) -> Box<Error> {
	Box::new(std::io::Error::new(ErrorKind::Other, message))
}

fn parse_u16(input: &mut Read, _: &Vec<Constant>) -> Result<u16, Box<Error>> {
	Ok(try!(input.read_u16::<BigEndian>()))
}

fn parse_list<T>(input: &mut Read, constants: &Vec<Constant>, parse_item: fn(&mut Read, &Vec<Constant>) -> Result<T,Box<Error>>  ) -> Result<Vec<T>, Box<Error>> {
	let count = try!(input.read_u16::<BigEndian>());
	(0 .. count).map(|_| parse_item(input, constants)).collect()
}


fn parse_constant(input: &mut Read) -> Result<Constant, Box<Error>> {
	let tag = try!(input.read_u8());
	match tag {
		1 => {
			let length = try!(input.read_u16::<BigEndian>()) as usize;
	    let mut bytes = vec![0u8; length];
			try!(input.read_exact(&mut bytes[..]));
			Ok(Constant::Utf8(try!(String::from_utf8(bytes))))
		},
		3 => {
			let int_value = try!(input.read_i32::<BigEndian>());
			Ok(Constant::Integer(int_value))
		},
		4 => {
			let float_value = try!(input.read_f32::<BigEndian>());
			Ok(Constant::Float(float_value))
		},
		5 => {
			let long_value = try!(input.read_i64::<BigEndian>());
			Ok(Constant::Long(long_value))
		},
		6 => {
			let double_value = try!(input.read_f64::<BigEndian>());
			Ok(Constant::Double(double_value))
		},
		7 => {
			let name_index = try!(input.read_u16::<BigEndian>());
			Ok(Constant::Class(name_index))
		},
		8 => {
			let string_index = try!(input.read_u16::<BigEndian>());
			Ok(Constant::String(string_index))
		},
		9 => {
			let class_index = try!(input.read_u16::<BigEndian>());
			let name_and_type_index = try!(input.read_u16::<BigEndian>());
			Ok(Constant::Fieldref(class_index, name_and_type_index))
		},
		10 => {
			let class_index = try!(input.read_u16::<BigEndian>());
			let name_and_type_index = try!(input.read_u16::<BigEndian>());
			Ok(Constant::Methodref(class_index, name_and_type_index))
		},
		11 => {
			let class_index = try!(input.read_u16::<BigEndian>());
			let name_and_type_index = try!(input.read_u16::<BigEndian>());
			Ok(Constant::InterfaceMethodref(class_index, name_and_type_index))
		},
		12 => {
			let name_index = try!(input.read_u16::<BigEndian>());
			let descriptor_index = try!(input.read_u16::<BigEndian>());
			Ok(Constant::NameAndType(name_index, descriptor_index))
		},
		15 => {
			Err(parse_error(&format!("Unsupported constant type: {:?}", Constant::MethodHandle)))
		}
		16 => {
			Err(parse_error(&format!("Unsupported constant type: {:?}", Constant::MethodType)))
		}
		18 => {
			Err(parse_error(&format!("Unsupported constant type: {:?}", Constant::InvokeDynamic)))
		}
		_ => Err(parse_error(&format!("Unsupported constant type: {}", tag)))
	}
}

fn field_type_to_stack_type(field_type: &FieldType) -> VerificationTypeInfo {
	match field_type {
		&FieldType::Byte => VerificationTypeInfo::Integer,
		&FieldType::Character => VerificationTypeInfo::Integer,
		&FieldType::Double => VerificationTypeInfo::Double,
		&FieldType::Float => VerificationTypeInfo::Float,
		&FieldType::Integer => VerificationTypeInfo::Integer,
		&FieldType::Long => VerificationTypeInfo::Long,
		&FieldType::Reference(ref name) => VerificationTypeInfo::Object(name.clone()),
		&FieldType::Short => VerificationTypeInfo::Integer,
		&FieldType::Boolean => VerificationTypeInfo::Integer,
		&FieldType::Array(ref element_type) => VerificationTypeInfo::Object(format!("[{:?}", element_type)),
	}
}

fn parse_field_type(input: &mut Peekable<Chars>) -> Result<FieldType, Box<Error>> {
	match input.next() {
		Some(c) => 	match c {
			'B' => Ok(FieldType::Byte),
			'C' => Ok(FieldType::Character),
			'D' => Ok(FieldType::Double),
			'F' => Ok(FieldType::Float),
			'I' => Ok(FieldType::Integer),
			'J' => Ok(FieldType::Long),
			'L' => {
				let type_name = input.take_while(|&c| c!=';').collect();
				Ok(FieldType::Reference(type_name))
			},
			'S' => Ok(FieldType::Short),
			'Z' => Ok(FieldType::Boolean),
			'[' => Ok(FieldType::Array(Box::new(try!(parse_field_type(input))))),
			_ => Err(parse_error(&format!("Unknown type: {}", c))),
		},
		None => 	Err(parse_error("Unexpected end of type signature")),
	}
}

fn parse_method_descriptor(input: &str) -> Result<MethodType, Box<Error>> {
	let mut i = input.chars().peekable();

	match i.next() {
		Some('(') => {},
		_ => return Err(parse_error(&format!("Method signature should start with '(': {}", input))),
	}
	let mut parameters = vec![];
	loop {
		match i.peek() {
			Some(&')') => {
				i.next();
				break
			}
			_ => parameters.push(try!(parse_field_type(&mut i))),
		}
	}
	let return_type = match i.peek() {
		Some(&'V') => {
			i.next();
			None
		}
		_ => Some(try!(parse_field_type(&mut i)))
	};
	Ok(MethodType(parameters, return_type))
}


fn parse_field(input: &mut Read, constants: &Vec<Constant>) -> Result<Field, Box<Error>> {
	let access_flags = try!(input.read_u16::<BigEndian>());
	let name_index = try!(input.read_u16::<BigEndian>());
	let descriptor = try!(parse_field_type(&mut get_string_constant(constants, try!(input.read_u16::<BigEndian>())).chars().peekable()));
	let mut constant_value = None;
	let mut signature = None;
	try!(parse_attributes(input, constants, |name, value| {
		return match name {
			"ConstantValue" => {
				constant_value = Some(try!(value.read_u16::<BigEndian>()));
				Ok(())
			},
			"Signature" => {
				let signature_index = try!(value.read_u16::<BigEndian>());
				signature = Some(get_string_constant(constants, signature_index).to_string());
				Ok(())
			},
			_ => Err(parse_error(&format!("Unknown field attribute: {}", name)))
		}
	}));
	Ok(Field{access_flags: access_flags, name_index: name_index, descriptor: descriptor, signature: signature, constant_value: constant_value})
}

fn parse_exception_table_entry(input: &mut Read, _: &Vec<Constant>) -> Result<ExceptionTableEntry, Box<Error>> {
	let start_pc = try!(input.read_u16::<BigEndian>());
	let end_pc = try!(input.read_u16::<BigEndian>());
	let handler_pc = try!(input.read_u16::<BigEndian>());
	let catch_type = try!(input.read_u16::<BigEndian>());
	Ok(ExceptionTableEntry{
		start_pc: start_pc,
		end_pc: end_pc,
		handler_pc: handler_pc,
		catch_type: catch_type,
	})
}

fn read_u8(bytes: &[u8], position: &mut usize) -> u8 {
	let b = bytes[*position];
	*position+=1;
	b
}

fn read_u16(bytes: &[u8], position: &mut usize) -> u16 {
	(read_u8(bytes, position) as u16) << 8u16 | (read_u8(bytes, position) as u16)
}

fn read_u32(bytes: &[u8], position: &mut usize) -> u32 {
	(read_u8(bytes, position) as u32) << 24u16 |
	(read_u8(bytes, position) as u32) << 16u16 |
	(read_u8(bytes, position) as u32) << 8u16 |
	(read_u8(bytes, position) as u32)
}

fn read_i8(bytes: &[u8], position: &mut usize) -> i8 {
	read_u8(bytes, position) as i8
}

fn read_i16(bytes: &[u8], position: &mut usize) -> i16 {
	read_u16(bytes, position) as i16
}

fn read_i32(bytes: &[u8], position: &mut usize) -> i32 {
	read_u32(bytes, position) as i32
}

impl StackType {
	fn is_long(&self) -> bool {
		match *self {
			StackType::Long|StackType::Double => true,
			StackType::Integer|StackType::Float|StackType::Reference => false,
			_ => panic!("Unknown stack type: {:?}", self),
		}
	}
}

fn parse_instructions(code: &[u8], constants: &Vec<Constant>, stack_map: &Vec<StackMapFrame>) -> Result<Vec<Instruction>, Box<Error>> {

	#[derive(Debug)]
	struct BasicBlock {
		instructions: Vec<Instruction>,
		stack: Vec<usize>,
		locals: BTreeMap<usize, usize>,
		terminator: Option<Instruction>,
	};

	impl BasicBlock {
		fn pop_value(&mut self) -> usize {
			self.stack.pop().unwrap()
		}
		fn pop_long_value(&mut self) -> usize {
			self.stack.pop().unwrap();
			self.stack.pop().unwrap()
		}
		fn push_value(&mut self, value: usize) {
			self.stack.push(value);
		}
		fn push_long_value(&mut self, value: usize) {
			self.stack.push(value);
			self.stack.push(!0);
		}
		fn push_result(&mut self, kind: StackType, value: usize) {
			if kind.is_long() {
				self.push_long_value(value);
			} else {
				self.push_value(value);
			}
		}
		fn push(&mut self, kind: StackType, instruction: Instruction) {
			let value = self.instructions.len();
			self.push_result(kind, value);
			self.instructions.push(instruction);
		}
		fn pop(&mut self, kind: StackType) -> usize {
			if kind.is_long() {
				self.pop_long_value()
			} else {
				self.pop_value()
			}
		}
		fn swap(&mut self, kind: StackType) {
			let v0 = self.pop(kind);
			let v1 = self.pop(kind);
			self.push_result(kind, v0);
			self.push_result(kind, v1);

		}
		fn load(&mut self, kind: StackType, id: u16) {
			let value = self.locals.get(&(id as usize)).unwrap();
			self.stack.push(*value);
			if kind.is_long() {
				self.stack.push(!0);
			}
		}
		fn store(&mut self, kind: StackType, id: u16) {
			let value = self.pop(kind);
			self.locals.insert(id as usize, value);
		}
		fn array_load(&mut self, kind: StackType) {
			let arrayref = self.pop(StackType::Reference);
			let index = self.pop(StackType::Integer);
			self.push(kind, Instruction::ArrayLoad(kind, arrayref, index));
		}
		fn array_store(&mut self, kind: StackType) {
			let arrayref = self.pop(StackType::Reference);
			let index = self.pop(StackType::Integer);
			let value = self.pop(kind);
			self.instructions.push(Instruction::ArrayStore(kind, arrayref, index, value));
		}

		fn binary_operation(&mut self, operation_kind: BinaryOperation, type_kind: StackType) {
			let left = self.pop(type_kind);
			let right = self.pop(type_kind);
			self.push(type_kind, Instruction::BinaryOperation(type_kind, operation_kind, left, right));
		}

		fn negate(&mut self, type_kind: StackType) {
			let value = self.pop(type_kind);
			self.push(type_kind, Instruction::Negate(type_kind, value));
		}

		fn convert(&mut self, from: StackType, to: StackType) {
			let value = self.pop(from);
			self.push(to, Instruction::Convert(from, to, value))
		}

		fn compare(&mut self, kind: StackType) {
			let left = self.pop(kind);
			let right = self.pop(kind);
			self.push(StackType::Integer, Instruction::Compare(kind, left, right))
		}

		fn terminate_return(&mut self, kind: StackType) {
			let value = self.pop(kind);
			self.terminator = Some(Instruction::Return(kind, value))
		}
		fn terminate_return_void(&mut self) {
			self.terminator = Some(Instruction::ReturnVoid)
		}
		fn terminate_goto(&mut self, destination: usize) {
			self.terminator = Some(Instruction::Goto(destination))
		}
		fn terminate_compare(&mut self, compare_type: CompareType, destination_then: usize, destination_else: usize) {
			self.compare(StackType::Integer);
			let cmp = self.pop(StackType::Integer);
			self.terminator = Some(Instruction::GotoIf(compare_type, cmp, destination_then, destination_else))
		}
		fn terminate_compare_reference(&mut self, compare_type: CompareType, destination_then: usize, destination_else: usize) {
			self.compare(StackType::Reference);
			let cmp = self.pop(StackType::Integer);
			self.terminator = Some(Instruction::GotoIf(compare_type, cmp, destination_then, destination_else))
		}
		fn terminate_compare_with_zero(&mut self, compare_type: CompareType, destination_then: usize, destination_else: usize) {
			let cmp = self.pop(StackType::Integer);
			self.terminator = Some(Instruction::GotoIf(compare_type, cmp, destination_then, destination_else))
		}
		fn terminate_compare_with_null(&mut self, compare_type: CompareType, destination_then: usize, destination_else: usize) {
			self.push(StackType::Reference, Instruction::NullConstant);
			self.swap(StackType::Reference);
			self.terminate_compare_reference(compare_type, destination_then, destination_else);
		}
		fn is_terminated(&self) -> bool {
			self.terminator.is_some()
		}
		fn new(initial_state: &StackMapFrame) -> BasicBlock {
			let mut bb = BasicBlock {
				instructions: vec![],
				stack: vec![],
				locals: BTreeMap::new(),
				terminator: None,
			};
			let mut n = 0;
			for local in initial_state.locals.iter() {
				let kind = local.to_stack_type();
				bb.push(kind, Instruction::Argument(n));
				bb.store(kind, n as u16);
				if kind.is_long() {
					n+=2;
				} else {
					n+=1;
				}
			}
			bb
		}
		fn continue_new(initial_state: &BasicBlock) -> BasicBlock{
			BasicBlock {
				instructions: vec![],
				stack: initial_state.stack.clone(),
				locals: initial_state.locals.clone(),
				terminator: None,
			}
		}
	}

	fn relative_jump(ip:usize, offset:i32) -> usize {
		((ip as i64) + (offset as i64)) as usize
	}

	fn push_constant(bb: &mut BasicBlock, constants: &Vec<Constant>, id: u16) {
		let constant = &constants[id as usize - 1];
		match *constant {
			Constant::Integer(n) => bb.push(StackType::Integer, Instruction::IntegerConstant(n)),
			Constant::Float(n) => bb.push(StackType::Float, Instruction::FloatConstant(n)),
			Constant::Long(n) => bb.push(StackType::Long, Instruction::LongConstant(n)),
			Constant::Double(n) => bb.push(StackType::Double, Instruction::DoubleConstant(n)),
			_ => panic!("Constant is not a value: {:?}", constant),
		}
	}

	let mut i = 0;
	let mut bb = BasicBlock::new(&stack_map[0]);
	let mut blocks = BTreeMap::new();
	let mut current_block_offset = i;

	while i < code.len() {
		let ip = i;
		let opcode = read_u8(code, &mut i);
		println!("{:x} {:#?} {:?}", opcode, bb, stack_map[0]);
		match opcode {
			0x00 => {},
			0x01 => bb.push(StackType::Reference, Instruction::NullConstant),
			0x02 => bb.push(StackType::Integer, Instruction::IntegerConstant(-1)),
			0x03 => bb.push(StackType::Integer, Instruction::IntegerConstant(0)),
			0x04 => bb.push(StackType::Integer, Instruction::IntegerConstant(1)),
			0x05 => bb.push(StackType::Integer, Instruction::IntegerConstant(2)),
			0x06 => bb.push(StackType::Integer, Instruction::IntegerConstant(3)),
			0x07 => bb.push(StackType::Integer, Instruction::IntegerConstant(4)),
			0x08 => bb.push(StackType::Integer, Instruction::IntegerConstant(5)),
			0x09 => bb.push(StackType::Long, Instruction::LongConstant(0)),
			0x0a => bb.push(StackType::Long, Instruction::LongConstant(1)),
			0x0b => bb.push(StackType::Float, Instruction::FloatConstant(0.0)),
			0x0c => bb.push(StackType::Float, Instruction::FloatConstant(1.0)),
			0x0d => bb.push(StackType::Float, Instruction::FloatConstant(2.0)),
			0x0e => bb.push(StackType::Double, Instruction::DoubleConstant(0.0)),
			0x0f => bb.push(StackType::Double, Instruction::DoubleConstant(1.0)),
			0x10 => bb.push(StackType::Integer, Instruction::IntegerConstant(read_i8(code, &mut i) as i32)),
			0x11 => bb.push(StackType::Integer, Instruction::IntegerConstant(read_i16(code, &mut i) as i32)),
			0x12 => push_constant(&mut bb, constants, read_u8(code, &mut i) as u16),
			0x13 => push_constant(&mut bb, constants, read_u16(code, &mut i) as u16),
			0x14 => push_constant(&mut bb, constants, read_u16(code, &mut i) as u16),
			0x15 => bb.load(StackType::Integer, read_u8(code, &mut i) as u16),
			0x16 => bb.load(StackType::Long, read_u8(code, &mut i) as u16),
			0x17 => bb.load(StackType::Float, read_u8(code, &mut i) as u16),
			0x18 => bb.load(StackType::Double, read_u8(code, &mut i) as u16),
			0x19 => bb.load(StackType::Reference, read_u8(code, &mut i) as u16),
			0x1a => bb.load(StackType::Integer, 0),
			0x1b => bb.load(StackType::Integer, 1),
			0x1c => bb.load(StackType::Integer, 2),
			0x1d => bb.load(StackType::Integer, 3),
			0x1e => bb.load(StackType::Long, 0),
			0x1f => bb.load(StackType::Long, 1),
			0x20 => bb.load(StackType::Long, 2),
			0x21 => bb.load(StackType::Long, 3),
			0x22 => bb.load(StackType::Float, 0),
			0x23 => bb.load(StackType::Float, 1),
			0x24 => bb.load(StackType::Float, 2),
			0x25 => bb.load(StackType::Float, 3),
			0x26 => bb.load(StackType::Double, 0),
			0x27 => bb.load(StackType::Double, 1),
			0x28 => bb.load(StackType::Double, 2),
			0x29 => bb.load(StackType::Double, 3),
			0x2a => bb.load(StackType::Reference, 0),
			0x2b => bb.load(StackType::Reference, 1),
			0x2c => bb.load(StackType::Reference, 2),
			0x2d => bb.load(StackType::Reference, 3),
			0x2e => bb.array_load(StackType::Integer),
			0x2f => bb.array_load(StackType::Long),
			0x30 => bb.array_load(StackType::Float),
			0x31 => bb.array_load(StackType::Double),
			0x32 => bb.array_load(StackType::Reference),
			0x33 => bb.array_load(StackType::Boolean),
			0x34 => bb.array_load(StackType::Character),
			0x35 => bb.array_load(StackType::Short),
			0x36 => bb.store(StackType::Integer, read_u8(code, &mut i) as u16),
			0x37 => bb.store(StackType::Long, read_u8(code, &mut i) as u16),
			0x38 => bb.store(StackType::Float, read_u8(code, &mut i) as u16),
			0x39 => bb.store(StackType::Double, read_u8(code, &mut i) as u16),
			0x3a => bb.store(StackType::Reference, read_u8(code, &mut i) as u16),
			0x3b => bb.store(StackType::Integer, 0),
			0x3c => bb.store(StackType::Integer, 1),
			0x3d => bb.store(StackType::Integer, 2),
			0x3e => bb.store(StackType::Integer, 3),
			0x3f => bb.store(StackType::Long, 0),
			0x40 => bb.store(StackType::Long, 1),
			0x41 => bb.store(StackType::Long, 2),
			0x42 => bb.store(StackType::Long, 3),
			0x43 => bb.store(StackType::Float, 0),
			0x44 => bb.store(StackType::Float, 1),
			0x45 => bb.store(StackType::Float, 2),
			0x46 => bb.store(StackType::Float, 3),
			0x47 => bb.store(StackType::Double, 0),
			0x48 => bb.store(StackType::Double, 1),
			0x49 => bb.store(StackType::Double, 2),
			0x4a => bb.store(StackType::Double, 3),
			0x4b => bb.store(StackType::Reference, 0),
			0x4c => bb.store(StackType::Reference, 1),
			0x4d => bb.store(StackType::Reference, 2),
			0x4e => bb.store(StackType::Reference, 3),
			0x4f => bb.array_store(StackType::Integer),
			0x50 => bb.array_store(StackType::Long),
			0x51 => bb.array_store(StackType::Float),
			0x52 => bb.array_store(StackType::Double),
			0x53 => bb.array_store(StackType::Reference),
			0x54 => bb.array_store(StackType::Byte),
			0x55 => bb.array_store(StackType::Character),
			0x56 => bb.array_store(StackType::Short),
			0x57 => {
				bb.pop_value();
			},
			0x58 => {
				bb.pop_long_value();
			}
			0x59 => {
				let value = bb.pop_value();
				bb.push_value(value);
				bb.push_value(value);
			},
			0x5a => {
				let value1 = bb.pop_value();
				let value2 = bb.pop_value();
				bb.push_value(value1);
				bb.push_value(value2);
				bb.push_value(value1);
			},
			0x5b => {
				let value1 = bb.pop_value();
				let value2 = bb.pop_value();
				let value3 = bb.pop_value();
				bb.push_value(value1);
				bb.push_value(value3);
				bb.push_value(value2);
				bb.push_value(value1);
			},
			0x5c => {
				let value = bb.pop_long_value();
				bb.push_long_value(value);
				bb.push_long_value(value);
			},
			0x5d => {
				let value1 = bb.pop_long_value();
				let value2 = bb.pop_long_value();
				bb.push_long_value(value1);
				bb.push_long_value(value2);
				bb.push_long_value(value1);
			},
			0x5e => {
				let value1 = bb.pop_long_value();
				let value2 = bb.pop_long_value();
				let value3 = bb.pop_long_value();
				bb.push_long_value(value1);
				bb.push_long_value(value3);
				bb.push_long_value(value2);
				bb.push_long_value(value1);
			},
			0x5f => {
				let value1 = bb.pop_value();
				let value2 = bb.pop_value();
				bb.push_value(value1);
				bb.push_value(value2);
			},
			0x60 => bb.binary_operation(BinaryOperation::Add, StackType::Integer),
			0x61 => bb.binary_operation(BinaryOperation::Add, StackType::Long),
			0x62 => bb.binary_operation(BinaryOperation::Add, StackType::Float),
			0x63 => bb.binary_operation(BinaryOperation::Add, StackType::Double),
			0x64 => bb.binary_operation(BinaryOperation::Subtract, StackType::Integer),
			0x65 => bb.binary_operation(BinaryOperation::Subtract, StackType::Long),
			0x66 => bb.binary_operation(BinaryOperation::Subtract, StackType::Float),
			0x67 => bb.binary_operation(BinaryOperation::Subtract, StackType::Double),
			0x68 => bb.binary_operation(BinaryOperation::Multiply, StackType::Integer),
			0x69 => bb.binary_operation(BinaryOperation::Multiply, StackType::Long),
			0x6a => bb.binary_operation(BinaryOperation::Multiply, StackType::Float),
			0x6b => bb.binary_operation(BinaryOperation::Multiply, StackType::Double),
			0x6c => bb.binary_operation(BinaryOperation::Divide, StackType::Integer),
			0x6d => bb.binary_operation(BinaryOperation::Divide, StackType::Long),
			0x6e => bb.binary_operation(BinaryOperation::Divide, StackType::Float),
			0x6f => bb.binary_operation(BinaryOperation::Divide, StackType::Double),
			0x70 => bb.binary_operation(BinaryOperation::Remainder, StackType::Integer),
			0x71 => bb.binary_operation(BinaryOperation::Remainder, StackType::Long),
			0x72 => bb.binary_operation(BinaryOperation::Remainder, StackType::Float),
			0x73 => bb.binary_operation(BinaryOperation::Remainder, StackType::Double),
			0x74 => bb.negate(StackType::Integer),
			0x75 => bb.negate(StackType::Long),
			0x76 => bb.negate(StackType::Float),
			0x77 => bb.negate(StackType::Double),
			0x78 => bb.binary_operation(BinaryOperation::LeftShift, StackType::Integer),
			0x79 => bb.binary_operation(BinaryOperation::LeftShift, StackType::Long),
			0x7a => bb.binary_operation(BinaryOperation::RightArithmeticShift, StackType::Integer),
			0x7b => bb.binary_operation(BinaryOperation::RightArithmeticShift, StackType::Long),
			0x7c => bb.binary_operation(BinaryOperation::RightLogicalShift, StackType::Integer),
			0x7d => bb.binary_operation(BinaryOperation::RightLogicalShift, StackType::Long),
			0x7e => bb.binary_operation(BinaryOperation::BitwiseAnd, StackType::Integer),
			0x7f => bb.binary_operation(BinaryOperation::BitwiseAnd, StackType::Long),
			0x80 => bb.binary_operation(BinaryOperation::BitwiseOr, StackType::Integer),
			0x81 => bb.binary_operation(BinaryOperation::BitwiseOr, StackType::Long),
			0x82 => bb.binary_operation(BinaryOperation::BitwiseXor, StackType::Integer),
			0x83 => bb.binary_operation(BinaryOperation::BitwiseXor, StackType::Long),
			0x84 => {
				let local_id = read_u8(code, &mut i) as u16;
				bb.load(StackType::Integer, local_id);
				bb.push(StackType::Integer, Instruction::IntegerConstant(read_i8(code, &mut i) as i32));
				bb.binary_operation(BinaryOperation::Add, StackType::Integer);
				bb.store(StackType::Integer, local_id);
			},
			0x85 => bb.convert(StackType::Integer, StackType::Long),
			0x86 => bb.convert(StackType::Integer, StackType::Float),
			0x87 => bb.convert(StackType::Integer, StackType::Double),
			0x88 => bb.convert(StackType::Long, StackType::Integer),
			0x89 => bb.convert(StackType::Long, StackType::Float),
			0x8a => bb.convert(StackType::Long, StackType::Double),
			0x8b => bb.convert(StackType::Float, StackType::Integer),
			0x8c => bb.convert(StackType::Float, StackType::Long),
			0x8d => bb.convert(StackType::Float, StackType::Double),
			0x8e => bb.convert(StackType::Double, StackType::Integer),
			0x8f => bb.convert(StackType::Double, StackType::Long),
			0x90 => bb.convert(StackType::Double, StackType::Float),
			0x91 => bb.convert(StackType::Integer, StackType::Byte),
			0x92 => bb.convert(StackType::Integer, StackType::Character),
			0x93 => bb.convert(StackType::Integer, StackType::Short),

			0x94 => bb.compare(StackType::Long),
			0x95 => bb.compare(StackType::Float),
			0x96 => bb.compare(StackType::Float),
			0x97 => bb.compare(StackType::Double),
			0x98 => bb.compare(StackType::Double),

			0x99 => bb.terminate_compare_with_zero(CompareType::EQ, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0x9a => bb.terminate_compare_with_zero(CompareType::NE, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0x9b => bb.terminate_compare_with_zero(CompareType::LT, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0x9c => bb.terminate_compare_with_zero(CompareType::GE, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0x9d => bb.terminate_compare_with_zero(CompareType::GT, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0x9e => bb.terminate_compare_with_zero(CompareType::LE, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0x9f => bb.terminate_compare(CompareType::EQ, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xa0 => bb.terminate_compare(CompareType::NE, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xa1 => bb.terminate_compare(CompareType::LT, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xa2 => bb.terminate_compare(CompareType::GE, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xa3 => bb.terminate_compare(CompareType::GT, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xa4 => bb.terminate_compare(CompareType::LE, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xa5 => bb.terminate_compare_reference(CompareType::EQ, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xa6 => bb.terminate_compare_reference(CompareType::NE, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xa7 => bb.terminate_goto(relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xac => bb.terminate_return(StackType::Integer),
			0xad => bb.terminate_return(StackType::Long),
			0xae => bb.terminate_return(StackType::Float),
			0xaf => bb.terminate_return(StackType::Double),
			0xb0 => bb.terminate_return(StackType::Reference),
			0xb1 => bb.terminate_return_void(),
/*
			0xb2 => Instruction::GetStatic(read_u16(code, &mut i)),
			0xb3 => Instruction::PutStatic(read_u16(code, &mut i)),
			0xb4 => Instruction::GetField(read_u16(code, &mut i)),
			0xb5 => Instruction::PutField(read_u16(code, &mut i)),
			0xb6 => Instruction::InvokeVirtual(read_u16(code, &mut i)),
			0xb7 => Instruction::InvokeSpecial(read_u16(code, &mut i)),
			0xb8 => Instruction::InvokeStatic(read_u16(code, &mut i)),
			0xb9 => Instruction::InvokeInterface(read_u16(code, &mut i)),
*/
			0xbb => bb.push(StackType::Reference, Instruction::New(read_u16(code, &mut i))),
/*
			0xbc => Instruction::NewArray(read_u8(code, &mut i)),
			0xbd => Instruction::NewReferenceArray(read_u16(code, &mut i)),
			0xbe => Instruction::ArrayLength,
			0xbf => Instruction::Throw,
			0xc0 => Instruction::CheckCast(read_u16(code, &mut i)),
			0xc1 => Instruction::InstanceOf(read_u16(code, &mut i)),
			0xc2 => Instruction::MonitorEnter,
			0xc3 => Instruction::MonitorExit,
*/
			0xc4 => match read_u8(code, &mut i) {
				0x15 => bb.load(StackType::Integer, read_u16(code, &mut i)),
				0x16 => bb.load(StackType::Long, read_u16(code, &mut i)),
				0x17 => bb.load(StackType::Float, read_u16(code, &mut i)),
				0x18 => bb.load(StackType::Double, read_u16(code, &mut i)),
				0x19 => bb.load(StackType::Reference, read_u16(code, &mut i)),
				0x36 => bb.store(StackType::Integer, read_u16(code, &mut i)),
				0x37 => bb.store(StackType::Long, read_u16(code, &mut i)),
				0x38 => bb.store(StackType::Float, read_u16(code, &mut i)),
				0x39 => bb.store(StackType::Double, read_u16(code, &mut i)),
				0x3a => bb.store(StackType::Reference, read_u16(code, &mut i)),
				0x84 => {
					let local_id = read_u16(code, &mut i);
					bb.load(StackType::Integer, local_id);
					bb.push(StackType::Integer, Instruction::IntegerConstant(read_i16(code, &mut i) as i32));
					bb.binary_operation(BinaryOperation::Add, StackType::Integer);
					bb.store(StackType::Integer, local_id);
				},
				_ => panic!("Unknown wide opcode"),
			},
			// 0xc5 => Instruction::MultiNewArray(read_u16(code, &mut i), read_u8(code, &mut i)),

			0xc6 => bb.terminate_compare_with_null(CompareType::EQ, relative_jump(ip, read_i16(code, &mut i) as i32), i),
			0xc7 => bb.terminate_compare_with_null(CompareType::NE, relative_jump(ip, read_i16(code, &mut i) as i32), i),

			0xc8 => bb.terminate_goto(relative_jump(ip, read_i32(code, &mut i))),
			_ => return Err(parse_error(&format!("Unknown opcode: 0x{:x}", opcode)))
		};
		if bb.is_terminated() {
			let next_bb = BasicBlock::continue_new(&bb);
			blocks.insert(current_block_offset, bb);

			bb = next_bb;
			current_block_offset = i;
			// stack_map[blocks.len()]
		}
	}
	println!("{:#?}", blocks);
	Ok(vec![Instruction::Nop; code.len()])
}

fn parse_verification_type_info(input: &mut Read, constants: &Vec<Constant>) -> Result<VerificationTypeInfo, Box<Error>> {
	let tag = try!(input.read_u8());
	match tag {
		0 => Ok(VerificationTypeInfo::Top),
		1 => Ok(VerificationTypeInfo::Integer),
		2 => Ok(VerificationTypeInfo::Float),
		3 => Ok(VerificationTypeInfo::Long),
		4 => Ok(VerificationTypeInfo::Double),
		5 => Ok(VerificationTypeInfo::Null),
		6 => Ok(VerificationTypeInfo::UninitializedThis),
		7 => {
			let class_id = try!(input.read_u16::<BigEndian>());
			Ok(VerificationTypeInfo::Object(get_class_constant(constants, class_id).to_string()))
		},
		8 => Ok(VerificationTypeInfo::Uninitialized),
		_ => Err(parse_error(&format!("Unknown verification type tag: {}", tag))),
	}
}

fn parse_stack_map_table(input: &mut Read, stack_map_table: &mut Vec<StackMapFrame>, constants: &Vec<Constant>) -> Result<(), Box<Error>> {
	let number_of_entries = try!(input.read_u16::<BigEndian>());
	let mut offset = 0;
	let mut stack = Vec::new();
	let mut locals = Vec::new();
	for _ in 0 .. number_of_entries {
		let id = try!(input.read_u8());
		match id {
			0...63 => {
				offset += id as u32;
			},
			64...127 => {
				offset += (id - 64) as u32;
				stack.clear();
				stack.push(try!(parse_verification_type_info(input, constants)));
			},
			247 => {
				let offset_delta = try!(input.read_u16::<BigEndian>());
				offset += offset_delta as u32;
				stack.clear();
				stack.push(try!(parse_verification_type_info(input, constants)));
			},
			248...250 => {
				let offset_delta = try!(input.read_u16::<BigEndian>());
				offset += offset_delta as u32;
				stack.clear();
				let new_locals_size = locals.len() + 251 - id as usize;
				locals.truncate(new_locals_size);
			},
			251 => {
				let offset_delta = try!(input.read_u16::<BigEndian>());
				offset += offset_delta as u32;
			},
			252...254 => {
				let added_locals = id - 251;
				let offset_delta = try!(input.read_u16::<BigEndian>());
				offset += offset_delta as u32;
				stack.clear();
				for _ in 0 .. added_locals {
					locals.push(try!(parse_verification_type_info(input, constants)));
				}
			},
			255 => {
				let offset_delta = try!(input.read_u16::<BigEndian>());
				offset += offset_delta as u32;
				locals.clear();
				let number_of_locals = try!(input.read_u16::<BigEndian>());
				for _ in 0 .. number_of_locals {
					locals.push(try!(parse_verification_type_info(input, constants)));
				}
				stack.clear();
				let number_of_stack_items = try!(input.read_u16::<BigEndian>());
				for _ in 0 .. number_of_stack_items {
					stack.push(try!(parse_verification_type_info(input, constants)));
				}
			},
			_ => return Err(parse_error(&format!("Unknown frame type: {}", id))),
		};
		let frame = StackMapFrame{offset: offset, locals: locals.clone(), stack: stack.clone()};
		offset += 1;
		stack_map_table.push(frame)
	}
	return Ok(());
}

fn parse_code(input: &mut Read, constants: &Vec<Constant>, signature: &MethodType) -> Result<Code, Box<Error>> {
	let max_stack = try!(input.read_u16::<BigEndian>());
	let max_locals = try!(input.read_u16::<BigEndian>());
	let code_length = try!(input.read_u32::<BigEndian>()) as usize;
	let mut code = vec![0; code_length];
	try!(input.read_exact(&mut code));
	let exception_table = try!(parse_list(input, constants, parse_exception_table_entry));
	let initial_stack = signature.0.iter().map(field_type_to_stack_type).collect();
	let mut stack_map_table = vec![StackMapFrame{offset: 0, locals: initial_stack, stack: vec![]}];
	try!(parse_attributes(input, constants, |name, value| {
		match name {
			"LineNumberTable" => {
				let mut bytes = Vec::new();
				try!(value.read_to_end(&mut bytes));
				Ok(())
			},
			"StackMapTable" => {
				try!(parse_stack_map_table(value, &mut stack_map_table, constants));
				Ok(())
			},
			_ => Err(parse_error(&format!("Unknown code attribute: {}", name)))
		}
	}));
	let basic_blocks = try!(parse_instructions(&code, constants, &stack_map_table));

	Ok(Code{
		max_stack: max_stack,
		max_locals: max_locals,
		instructions: BTreeMap::new(),
		exception_table: exception_table,
	})
}


fn parse_method(input: &mut Read, constants: &Vec<Constant>) -> Result<Method, Box<Error>> {
	let access_flags = try!(input.read_u16::<BigEndian>());
	let name_index = try!(input.read_u16::<BigEndian>());
	let descriptor = try!(parse_method_descriptor(get_string_constant(constants, try!(input.read_u16::<BigEndian>()))));
	let mut code = None;
	let mut signature = None;
	let mut exceptions = vec![];
	try!(parse_attributes(input, constants, |name, value| {
		match name {
			"Code" => {
				code = Some(try!(parse_code(value, constants, &descriptor)));
				Ok(())
			},
			"Signature" => {
				let signature_index = try!(value.read_u16::<BigEndian>());
				signature = Some(get_string_constant(constants, signature_index).to_string());
				Ok(())
			},
			"Exceptions" => {
				let number_of_exceptions = try!(value.read_u16::<BigEndian>());
				for _ in 0 .. number_of_exceptions {
					let exception_id = try!(value.read_u16::<BigEndian>());
					let exception = get_class_constant(constants, exception_id).to_string();
					exceptions.push(exception);
				}
				Ok(())
			},
			_ => Err(parse_error(&format!("Unknown method attribute: {}", name)))
		}
	}));
	Ok(Method{access_flags: access_flags, name_index: name_index, descriptor: descriptor, signature: signature, exceptions: exceptions, code: code})
}

fn parse_attributes<F>(input: &mut Read, constants: &Vec<Constant>, mut f : F) -> Result<(), Box<Error>> where 
	F: FnMut(&str, &mut Read) -> Result<(), Box<Error>> {
	let count = try!(input.read_u16::<BigEndian>());
	for _ in 0 .. count {
		let name_index = try!(input.read_u16::<BigEndian>());
		let name = get_string_constant(constants, name_index);
		let length = try!(input.read_u32::<BigEndian>()) as u64;
		let mut value = input.take(length);
		try!(f(name, &mut value));
	}
	Ok(())
}

fn parse_class(input: &mut Read) -> Result<ClassFile, Box<Error>> {
	let magic = try!(input.read_u32::<BigEndian>());
	if magic != 0xcafebabe {
		return Err(parse_error(&format!("Bad magic number: {:08x}", magic)));
	}

	let minor_version = try!(input.read_u16::<BigEndian>());
	let major_version = try!(input.read_u16::<BigEndian>());
	if major_version > 52 {
		return Err(parse_error(&format!("Unsupported class version: {}.{}", major_version, minor_version)));
	}

	let constant_pool_count = try!(input.read_u16::<BigEndian>()) as usize - 1;
	let mut constant_pool = Vec::with_capacity(constant_pool_count);
	while constant_pool.len() != constant_pool_count {
		let constant = try!(parse_constant(input));
		match constant {
			Constant::Long(_)|Constant::Double(_) => {
				constant_pool.push(constant);
				constant_pool.push(Constant::Unusable);
			},
			_ => {
				constant_pool.push(constant);
			},
		}
	}
	let access_flags = try!(input.read_u16::<BigEndian>());
	let this_class = try!(input.read_u16::<BigEndian>());
	let super_class = try!(input.read_u16::<BigEndian>());

	let interfaces = try!(parse_list(input, &constant_pool, parse_u16));
	let fields = try!(parse_list(input, &constant_pool, parse_field));
	let methods = try!(parse_list(input, &constant_pool, parse_method));
	let mut source_file = None;
	try!(parse_attributes(input, &constant_pool, |name, value| {
		return match name {
			"SourceFile" => {
				source_file = Some(try!(value.read_u16::<BigEndian>()));
				Ok(())
			},
			_ => Err(parse_error(&format!("Unknown class attribute: {}", name)))
		}
	}));

	Ok(ClassFile{
		constant_pool: constant_pool,
		access_flags: access_flags,
		this_class: this_class,
		super_class: super_class,
		interfaces: interfaces,
		fields: fields,
		methods: methods,
		source_file: source_file,
	})
}

fn parse_class_file(file_name: &str) -> Result<ClassFile, Box<Error>> {
	let mut file = try!(File::open(file_name));
	parse_class(&mut file)
}

fn main() {
	for arg in env::args().skip(1) {
		match  parse_class_file(&arg) {
			Err(e) => {println!("{}", e); break;},
			Ok(v) => {},
		}
	}
}
