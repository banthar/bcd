extern crate byteorder;

use std::error::Error;
use std::env;
use std::fs::File;
use std::io::{Read, ErrorKind};
use byteorder::{BigEndian, ReadBytesExt};

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
	descriptor_index: u16,
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
enum ShiftType {
	Left,
	RightArithmetic,
	RightLogical,
}

#[derive(Debug,Clone)]
enum Instruction {
	Nop,
	IntegerConstant(i32),
	LongConstant(i64),
	FloatConstant(f32),
	DoubleConstant(f64),
	NullConstant,
	ArrayLoad(StackType),
	ArrayStore(StackType),
	LoadConstant(u16),
	Load(StackType, u16),
	Store(StackType, u16),
	ReturnVoid,
	Return(StackType),
	GetStatic(u16),
	Pop,
	Pop2,
	Dup,
	DupX1,
	DupX2,
	Dup2,
	Dup2X1,
	Dup2X2,
	Swap,
	Add(StackType),
	Subtract(StackType),
	Multiply(StackType),
	Divide(StackType),
	Remainder(StackType),
	Negate(StackType),
	Shift(ShiftType, StackType),
	BitwiseAnd(StackType),
	BitwiseOr(StackType),
	BitwiseXor(StackType),
	New(u16),
	Throw,
	Increment(u16, i32),
	Convert(StackType, StackType),
	GetField(u16),
	PutField(u16),
	PutStatic(u16),
	InvokeVirtual(u16),
	InvokeSpecial(u16),
	InvokeStatic(u16),
	InvokeInterface(u16),
	Compare(StackType),
	IfCompareZero(StackType, CompareType, usize),
	IfCompare(StackType, CompareType, usize),
	Goto(usize),
	NewArray(u8),
	NewReferenceArray(u16),
	CheckCast(u16),
	InstanceOf(u16),
	MonitorEnter,
	MonitorExit,
	ArrayLength,
	MultiNewArray(u16, u8),
}

#[derive(Debug)]
struct Code {
	max_stack: u16,
	max_locals: u16,
	stack_map_table: Vec<u8>,
	instructions: Vec<Instruction>,
	exception_table: Vec<ExceptionTableEntry>,
}

#[derive(Debug)]
struct Method {
	access_flags: u16,
	name_index: u16,
	descriptor_index: u16,
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

fn parse_field(input: &mut Read, constants: &Vec<Constant>) -> Result<Field, Box<Error>> {
	let access_flags = try!(input.read_u16::<BigEndian>());
	let name_index = try!(input.read_u16::<BigEndian>());
	let descriptor_index = try!(input.read_u16::<BigEndian>());
	let mut constant_value = None;
	let mut signature = None;
	try!(parse_attributes(input, constants, |name, value| {
		return match name {
			"ConstantValue" => {
				constant_value = Some(try!(value.read_u16::<BigEndian>()));
				Ok(())
			},
			"Signature" => {
				let mut s = String::new();
				try!(value.read_to_string(&mut s));
				signature = Some(s);
				Ok(())
			},
			_ => Err(parse_error(&format!("Unknown field attribute: {}", name)))
		}
	}));
	Ok(Field{access_flags: access_flags, name_index: name_index, descriptor_index: descriptor_index, constant_value: constant_value})
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

fn relative_jump(ip:usize, offset:i32) -> usize {
	((ip as i64) + (offset as i64)) as usize
}

fn parse_instructions(code: &[u8], constants: &Vec<Constant>) -> Result<Vec<Instruction>, Box<Error>> {
	let mut instructions = vec![Instruction::Nop; code.len()];
	let mut i = 0;
	while i < code.len() {
		let ip = i;
		let opcode = read_u8(code, &mut i);
		instructions[ip] = match opcode {
			0x00 => Instruction::Nop,
			0x01 => Instruction::NullConstant,
			0x02 => Instruction::IntegerConstant(-1),
			0x03 => Instruction::IntegerConstant(0),
			0x04 => Instruction::IntegerConstant(1),
			0x05 => Instruction::IntegerConstant(2),
			0x06 => Instruction::IntegerConstant(3),
			0x07 => Instruction::IntegerConstant(4),
			0x08 => Instruction::IntegerConstant(5),
			0x09 => Instruction::LongConstant(0),
			0x0a => Instruction::LongConstant(1),
			0x0b => Instruction::FloatConstant(0.0),
			0x0c => Instruction::FloatConstant(1.0),
			0x0d => Instruction::FloatConstant(2.0),
			0x0e => Instruction::DoubleConstant(0.0),
			0x0f => Instruction::DoubleConstant(1.0),
			0x10 => Instruction::IntegerConstant(read_u8(code, &mut i) as i32),
			0x11 => Instruction::IntegerConstant(read_u16(code, &mut i) as i32),
			0x12 => Instruction::LoadConstant(read_u8(code, &mut i) as u16),
			0x13 => Instruction::LoadConstant(read_u16(code, &mut i) as u16),
			0x14 => Instruction::LoadConstant(read_u16(code, &mut i) as u16),
			0x15 => Instruction::Load(StackType::Integer, read_u8(code, &mut i) as u16),
			0x16 => Instruction::Load(StackType::Long, read_u8(code, &mut i) as u16),
			0x17 => Instruction::Load(StackType::Float, read_u8(code, &mut i) as u16),
			0x18 => Instruction::Load(StackType::Double, read_u8(code, &mut i) as u16),
			0x19 => Instruction::Load(StackType::Reference, read_u8(code, &mut i) as u16),
			0x1a => Instruction::Load(StackType::Integer, 0),
			0x1b => Instruction::Load(StackType::Integer, 1),
			0x1c => Instruction::Load(StackType::Integer, 2),
			0x1d => Instruction::Load(StackType::Integer, 3),
			0x1e => Instruction::Load(StackType::Long, 0),
			0x1f => Instruction::Load(StackType::Long, 1),
			0x20 => Instruction::Load(StackType::Long, 2),
			0x21 => Instruction::Load(StackType::Long, 3),
			0x22 => Instruction::Load(StackType::Float, 0),
			0x23 => Instruction::Load(StackType::Float, 1),
			0x24 => Instruction::Load(StackType::Float, 2),
			0x25 => Instruction::Load(StackType::Float, 3),
			0x26 => Instruction::Load(StackType::Double, 0),
			0x27 => Instruction::Load(StackType::Double, 1),
			0x28 => Instruction::Load(StackType::Double, 2),
			0x29 => Instruction::Load(StackType::Double, 3),
			0x2a => Instruction::Load(StackType::Reference, 0),
			0x2b => Instruction::Load(StackType::Reference, 1),
			0x2c => Instruction::Load(StackType::Reference, 2),
			0x2d => Instruction::Load(StackType::Reference, 3),
			0x2e => Instruction::ArrayLoad(StackType::Integer),
			0x2f => Instruction::ArrayLoad(StackType::Long),
			0x30 => Instruction::ArrayLoad(StackType::Float),
			0x31 => Instruction::ArrayLoad(StackType::Double),
			0x32 => Instruction::ArrayLoad(StackType::Reference),
			0x33 => Instruction::ArrayLoad(StackType::Boolean),
			0x34 => Instruction::ArrayLoad(StackType::Character),
			0x35 => Instruction::ArrayLoad(StackType::Short),
			0x36 => Instruction::Store(StackType::Integer, read_u8(code, &mut i) as u16),
			0x37 => Instruction::Store(StackType::Long, read_u8(code, &mut i) as u16),
			0x38 => Instruction::Store(StackType::Float, read_u8(code, &mut i) as u16),
			0x39 => Instruction::Store(StackType::Double, read_u8(code, &mut i) as u16),
			0x3a => Instruction::Store(StackType::Reference, read_u8(code, &mut i) as u16),
			0x3b => Instruction::Store(StackType::Integer, 0),
			0x3c => Instruction::Store(StackType::Integer, 1),
			0x3d => Instruction::Store(StackType::Integer, 2),
			0x3e => Instruction::Store(StackType::Integer, 3),
			0x3f => Instruction::Store(StackType::Long, 0),
			0x40 => Instruction::Store(StackType::Long, 1),
			0x41 => Instruction::Store(StackType::Long, 2),
			0x42 => Instruction::Store(StackType::Long, 3),
			0x43 => Instruction::Store(StackType::Float, 0),
			0x44 => Instruction::Store(StackType::Float, 1),
			0x45 => Instruction::Store(StackType::Float, 2),
			0x46 => Instruction::Store(StackType::Float, 3),
			0x47 => Instruction::Store(StackType::Double, 0),
			0x48 => Instruction::Store(StackType::Double, 1),
			0x49 => Instruction::Store(StackType::Double, 2),
			0x4a => Instruction::Store(StackType::Double, 3),
			0x4b => Instruction::Store(StackType::Reference, 0),
			0x4c => Instruction::Store(StackType::Reference, 1),
			0x4d => Instruction::Store(StackType::Reference, 2),
			0x4e => Instruction::Store(StackType::Reference, 3),
			0x4f => Instruction::ArrayStore(StackType::Integer),
			0x50 => Instruction::ArrayStore(StackType::Long),
			0x51 => Instruction::ArrayStore(StackType::Float),
			0x52 => Instruction::ArrayStore(StackType::Double),
			0x53 => Instruction::ArrayStore(StackType::Reference),
			0x54 => Instruction::ArrayStore(StackType::Byte),
			0x55 => Instruction::ArrayStore(StackType::Character),
			0x56 => Instruction::ArrayStore(StackType::Short),
			0x57 => Instruction::Pop,
			0x58 => Instruction::Pop2,
			0x59 => Instruction::Dup,
			0x5a => Instruction::DupX1,
			0x5b => Instruction::DupX2,
			0x5c => Instruction::Dup2,
			0x5d => Instruction::Dup2X1,
			0x5e => Instruction::Dup2X2,
			0x5f => Instruction::Swap,
			0x60 => Instruction::Add(StackType::Integer),
			0x61 => Instruction::Add(StackType::Long),
			0x62 => Instruction::Add(StackType::Float),
			0x63 => Instruction::Add(StackType::Double),
			0x64 => Instruction::Subtract(StackType::Integer),
			0x65 => Instruction::Subtract(StackType::Long),
			0x66 => Instruction::Subtract(StackType::Float),
			0x67 => Instruction::Subtract(StackType::Double),
			0x68 => Instruction::Multiply(StackType::Integer),
			0x69 => Instruction::Multiply(StackType::Long),
			0x6a => Instruction::Multiply(StackType::Float),
			0x6b => Instruction::Multiply(StackType::Double),
			0x6c => Instruction::Divide(StackType::Integer),
			0x6d => Instruction::Divide(StackType::Long),
			0x6e => Instruction::Divide(StackType::Float),
			0x6f => Instruction::Divide(StackType::Double),
			0x70 => Instruction::Remainder(StackType::Integer),
			0x71 => Instruction::Remainder(StackType::Long),
			0x72 => Instruction::Remainder(StackType::Float),
			0x73 => Instruction::Remainder(StackType::Double),
			0x74 => Instruction::Negate(StackType::Integer),
			0x75 => Instruction::Negate(StackType::Long),
			0x76 => Instruction::Negate(StackType::Float),
			0x77 => Instruction::Negate(StackType::Double),
			0x78 => Instruction::Shift(ShiftType::Left, StackType::Integer),
			0x79 => Instruction::Shift(ShiftType::Left, StackType::Long),
			0x7a => Instruction::Shift(ShiftType::RightArithmetic, StackType::Integer),
			0x7b => Instruction::Shift(ShiftType::RightArithmetic, StackType::Long),
			0x7c => Instruction::Shift(ShiftType::RightLogical, StackType::Integer),
			0x7d => Instruction::Shift(ShiftType::RightLogical, StackType::Long),
			0x7e => Instruction::BitwiseAnd(StackType::Integer),
			0x7f => Instruction::BitwiseAnd(StackType::Long),
			0x80 => Instruction::BitwiseOr(StackType::Integer),
			0x81 => Instruction::BitwiseOr(StackType::Long),
			0x82 => Instruction::BitwiseXor(StackType::Integer),
			0x83 => Instruction::BitwiseXor(StackType::Long),
			0x84 => Instruction::Increment(read_u8(code, &mut i) as u16, read_i8(code, &mut i) as i32),
			0x85 => Instruction::Convert(StackType::Integer, StackType::Long),
			0x86 => Instruction::Convert(StackType::Integer, StackType::Float),
			0x87 => Instruction::Convert(StackType::Integer, StackType::Double),
			0x88 => Instruction::Convert(StackType::Long, StackType::Integer),
			0x89 => Instruction::Convert(StackType::Long, StackType::Float),
			0x8a => Instruction::Convert(StackType::Long, StackType::Double),
			0x8b => Instruction::Convert(StackType::Float, StackType::Integer),
			0x8c => Instruction::Convert(StackType::Float, StackType::Long),
			0x8d => Instruction::Convert(StackType::Float, StackType::Double),
			0x8e => Instruction::Convert(StackType::Double, StackType::Integer),
			0x8f => Instruction::Convert(StackType::Double, StackType::Long),
			0x90 => Instruction::Convert(StackType::Double, StackType::Float),
			0x91 => Instruction::Convert(StackType::Integer, StackType::Byte),
			0x92 => Instruction::Convert(StackType::Integer, StackType::Character),
			0x93 => Instruction::Convert(StackType::Integer, StackType::Short),
			0x94 => Instruction::Compare(StackType::Long),
			0x95 => Instruction::Compare(StackType::Float),
			0x96 => Instruction::Compare(StackType::Float),
			0x97 => Instruction::Compare(StackType::Double),
			0x98 => Instruction::Compare(StackType::Double),
			0x99 => Instruction::IfCompareZero(StackType::Integer, CompareType::EQ, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0x9a => Instruction::IfCompareZero(StackType::Integer, CompareType::NE, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0x9b => Instruction::IfCompareZero(StackType::Integer, CompareType::LT, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0x9c => Instruction::IfCompareZero(StackType::Integer, CompareType::GE, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0x9d => Instruction::IfCompareZero(StackType::Integer, CompareType::GT, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0x9e => Instruction::IfCompareZero(StackType::Integer, CompareType::LE, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0x9f => Instruction::IfCompare(StackType::Integer, CompareType::EQ, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xa0 => Instruction::IfCompare(StackType::Integer, CompareType::NE, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xa1 => Instruction::IfCompare(StackType::Integer, CompareType::LT, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xa2 => Instruction::IfCompare(StackType::Integer, CompareType::GE, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xa3 => Instruction::IfCompare(StackType::Integer, CompareType::GT, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xa4 => Instruction::IfCompare(StackType::Integer, CompareType::LE, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xa5 => Instruction::IfCompare(StackType::Reference, CompareType::EQ, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xa6 => Instruction::IfCompare(StackType::Reference, CompareType::NE, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xa7 => Instruction::Goto(relative_jump(ip, read_i16(code, &mut i) as i32)),

			0xac => Instruction::Return(StackType::Integer),
			0xad => Instruction::Return(StackType::Long),
			0xae => Instruction::Return(StackType::Float),
			0xaf => Instruction::Return(StackType::Double),
			0xb0 => Instruction::Return(StackType::Reference),
			0xb1 => Instruction::ReturnVoid,
			0xb2 => Instruction::GetStatic(read_u16(code, &mut i)),
			0xb3 => Instruction::PutStatic(read_u16(code, &mut i)),
			0xb4 => Instruction::GetField(read_u16(code, &mut i)),
			0xb5 => Instruction::PutField(read_u16(code, &mut i)),
			0xb6 => Instruction::InvokeVirtual(read_u16(code, &mut i)),
			0xb7 => Instruction::InvokeSpecial(read_u16(code, &mut i)),
			0xb8 => Instruction::InvokeStatic(read_u16(code, &mut i)),
			0xb9 => Instruction::InvokeInterface(read_u16(code, &mut i)),

			0xbb => Instruction::New(read_u16(code, &mut i)),
			0xbc => Instruction::NewArray(read_u8(code, &mut i)),
			0xbd => Instruction::NewReferenceArray(read_u16(code, &mut i)),
			0xbe => Instruction::ArrayLength,
			0xbf => Instruction::Throw,
			0xc0 => Instruction::CheckCast(read_u16(code, &mut i)),
			0xc1 => Instruction::InstanceOf(read_u16(code, &mut i)),
			0xc2 => Instruction::MonitorEnter,
			0xc3 => Instruction::MonitorExit,
			0xc4 => match read_u8(code, &mut i) {
				0x15 => Instruction::Load(StackType::Integer, read_u16(code, &mut i)),
				0x16 => Instruction::Load(StackType::Long, read_u16(code, &mut i)),
				0x17 => Instruction::Load(StackType::Float, read_u16(code, &mut i)),
				0x18 => Instruction::Load(StackType::Double, read_u16(code, &mut i)),
				0x19 => Instruction::Load(StackType::Reference, read_u16(code, &mut i)),
				0x36 => Instruction::Store(StackType::Integer, read_u16(code, &mut i)),
				0x37 => Instruction::Store(StackType::Long, read_u16(code, &mut i)),
				0x38 => Instruction::Store(StackType::Float, read_u16(code, &mut i)),
				0x39 => Instruction::Store(StackType::Double, read_u16(code, &mut i)),
				0x3a => Instruction::Store(StackType::Reference, read_u16(code, &mut i)),
				0x84 => Instruction::Increment(read_u16(code, &mut i), read_i16(code, &mut i) as i32),
				_ => panic!("Unknown wide opcode"),
			},
			0xc5 => Instruction::MultiNewArray(read_u16(code, &mut i), read_u8(code, &mut i)),

			0xc6 => Instruction::IfCompareZero(StackType::Reference, CompareType::EQ, relative_jump(ip, read_i16(code, &mut i) as i32)),
			0xc7 => Instruction::IfCompareZero(StackType::Reference, CompareType::NE, relative_jump(ip, read_i16(code, &mut i) as i32)),

			0xc8 => Instruction::Goto(relative_jump(ip, read_i32(code, &mut i))),


			_ => return Err(parse_error(&format!("Unknown opcode: 0x{:x}", opcode)))
		};
	}
	Ok(instructions)
}

fn parse_code(input: &mut Read, constants: &Vec<Constant>) -> Result<Code, Box<Error>> {
	let max_stack = try!(input.read_u16::<BigEndian>());
	let max_locals = try!(input.read_u16::<BigEndian>());
	let code_length = try!(input.read_u32::<BigEndian>()) as usize;
	let mut code = vec![0; code_length];
	try!(input.read_exact(&mut code));
	let instructions = try!(parse_instructions(&code, constants));
	let exception_table = try!(parse_list(input, constants, parse_exception_table_entry));
	let mut stack_map_table = Vec::new();
	try!(parse_attributes(input, constants, |name, value| {
		match name {
			"LineNumberTable" => {
				let mut bytes = Vec::new();
				try!(value.read_to_end(&mut bytes));
				Ok(())
			},
			"StackMapTable" => {
				let mut bytes = Vec::new();
				try!(value.read_to_end(&mut bytes));
				stack_map_table = bytes;
				Ok(())
			},
			_ => Err(parse_error(&format!("Unknown code attribute: {}", name)))
		}
	}));
	Ok(Code{
		max_stack: max_stack,
		max_locals: max_locals,
		stack_map_table: stack_map_table,
		instructions: instructions,
		exception_table: exception_table,
	})
}


fn parse_method(input: &mut Read, constants: &Vec<Constant>) -> Result<Method, Box<Error>> {
	let access_flags = try!(input.read_u16::<BigEndian>());
	let name_index = try!(input.read_u16::<BigEndian>());
	let descriptor_index = try!(input.read_u16::<BigEndian>());
	let mut code = None;
	try!(parse_attributes(input, constants, |name, value| {
		match name {
			"Code" => {
				code = Some(try!(parse_code(value, constants)));
				Ok(())
			},
			_ => Err(parse_error(&format!("Unknown method attribute: {}", name)))
		}
	}));
	Ok(Method{access_flags: access_flags, name_index: name_index, descriptor_index: descriptor_index, code: code})
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

struct Delexer {
	line_start: bool,
	indent: u32,
}

impl Delexer {
	pub fn new() -> Delexer {
		Delexer {
			line_start: true,
			indent: 0,
		}
	}
	pub fn token(&mut self, lex: &str) {
		if !self.line_start {
			print!(" ");
		} else {
			for _ in 0 .. self.indent {
				print!("\t");
			}
		}
		print!("{}", lex);
		self.line_start = false;
	}
	pub fn separator(&mut self, lex: &str) {
		print!("{}", lex);
	}
	pub fn end_line(&mut self) {
		print!("\n");
		self.line_start = true;
	}
	pub fn start_bracket(&mut self) {
		self.token("{");
		self.end_line();
		self.indent += 1;
	}
	pub fn end_bracket(&mut self) {
		self.indent -= 1;
		self.token("}");
		self.end_line();
	}
}

fn dump_field(class: &ClassFile, field: &Field, delexer: &mut Delexer) {
	if field.access_flags & 0x0001 == 0x0001 {
		delexer.token("public");
	}
	if field.access_flags & 0x0002 == 0x0002 {
		delexer.token("private");
	}
	if field.access_flags & 0x0004 == 0x0004 {
		delexer.token("protected");
	}
	if field.access_flags & 0x0008 == 0x0008 {
		delexer.token("static");
	}
	if field.access_flags & 0x0010 == 0x0010 {
		delexer.token("final");
	}
	if field.access_flags & 0x0040 == 0x0040 {
		delexer.token("volatile");
	}
	if field.access_flags & 0x0080 == 0x0080 {
		delexer.token("transient");
	}
	delexer.token(get_string_constant(&class.constant_pool, field.descriptor_index));
	delexer.token(get_string_constant(&class.constant_pool, field.name_index));

	match field.constant_value {
		Some(constant_id) => {
			delexer.token("=");
			delexer.token(&format!("{:?}", class.constant_pool[constant_id as usize - 1]));
		}
		None => {},
	}
	delexer.separator(";");
	delexer.end_line();
}

fn dump_method(class: &ClassFile, method: &Method, delexer: &mut Delexer) {
	if method.access_flags & 0x0001 == 0x0001 {
		delexer.token("public");
	}
	delexer.token(get_string_constant(&class.constant_pool, method.descriptor_index));
	delexer.token(get_string_constant(&class.constant_pool, method.name_index));
	match method.code {
		Some(ref code) => { 
			delexer.start_bracket();
			let mut i = 0;
			for instruction in &code.instructions {
				match *instruction {
					Instruction::Nop => {},
					_ => {
						delexer.token(&format!("{:4}: {:?}", i, instruction));
						delexer.separator(";");
						delexer.end_line();
					}
				}
				i+=1;
			}
			delexer.end_bracket();
		},
		None => delexer.separator(";"),
	}
	delexer.end_line();
}

fn dump_class(class: &ClassFile, delexer: &mut Delexer) {
	if (class.access_flags & 0x0001) == 0x0001 {
		delexer.token("public");
	}
	if (class.access_flags & 0x0010) == 0x0010 {
		delexer.token("final");
	}
	match class.access_flags & 0x6600 {
		0x0000 => delexer.token("class"),
		0x0600 => delexer.token("interface"),
		0x2000 => delexer.token("@interface"),
		0x4000 => delexer.token("enum"),
		_ => panic!("Unsupported class access flags: 0x{:04x}", class.access_flags)
	}
	delexer.token(get_class_constant(&class.constant_pool, class.this_class));
	delexer.start_bracket();

	match class.source_file {
		Some(source_file) => { 
			delexer.token("//");
			delexer.token(get_string_constant(&class.constant_pool, source_file));
			delexer.end_line();
		}
		None => ()
	}

	for field in &class.fields {
		dump_field(class, &field, delexer);
	}
	for method in &class.methods {
		dump_method(class, &method, delexer);
	}
	delexer.end_bracket();
}

fn main() {
	for arg in env::args().skip(1) {
		match  parse_class_file(&arg) {
			Err(e) => {println!("{}", e); break;},
			Ok(v) => {
				dump_class(&v, &mut Delexer::new());
			}
		}
	}
}
