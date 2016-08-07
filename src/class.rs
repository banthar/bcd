
extern crate byteorder;

use std::error::Error;
use std;
use std::io::{Read, ErrorKind};
use self::byteorder::{BigEndian, ReadBytesExt};
use std::iter::Peekable;
use std::str::Chars;

#[derive(Debug)]
pub enum Constant {
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
pub struct Field {
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

#[derive(Debug, Clone)]
pub enum VerificationTypeInfo {
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

#[derive(Debug, Clone)]
pub enum FieldType {
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
pub struct StackMapFrame {
	pub offset: u32,
	pub stack: Vec<VerificationTypeInfo>,
	pub locals: Vec<VerificationTypeInfo>,
}

#[derive(Debug)]
pub struct Code {
	max_stack: u16,
	max_locals: u16,
	pub instructions: Vec<u8>,
	pub stack_map: Vec<StackMapFrame>,
	exception_table: Vec<ExceptionTableEntry>,
}

#[derive(Debug)]
pub struct Method {
	access_flags: u16,
	name_index: u16,
	exceptions: Vec<String>,
	descriptor: MethodType,
	signature: Option<String>,
	pub code: Option<Code>
}

#[derive(Debug)]
pub struct ClassFile {
	pub constant_pool: Vec<Constant>,
	access_flags: u16,
	this_class: u16,
	super_class: u16,
	interfaces: Vec<u16>,
	pub fields: Vec<Field>,
	pub methods: Vec<Method>,
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

	Ok(Code{
		max_stack: max_stack,
		max_locals: max_locals,
		instructions: code,
		stack_map: stack_map_table,
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

impl ClassFile {
	pub fn read(input: &mut Read) -> Result<ClassFile, Box<Error>> {
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
}

