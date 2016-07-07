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
enum Instruction {
	Nop,
	IntegerConstant(u32),
	LoadConstant(u16),
	Load(StackType, u8),
	Add(StackType),
	Store(StackType, u8),
	InvokeSpecial(u16),
	InvokeVirtual(u16),
	Return,
	GetStatic(u16),
	Dup,
	New(u16),
	Throw,
	Increment(u8, u32),
	PutField(u16),
	PutStatic(u16),
	Compare(StackType, CompareType, usize),
	Goto(usize),
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

fn read_i16(bytes: &[u8], position: &mut usize) -> i16 {
	read_u16(bytes, position) as i16
}

fn relative_jump(ip:usize, offset:i16) -> usize {
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
			0x03 => Instruction::IntegerConstant(0),
			0x04 => Instruction::IntegerConstant(1),
			0x05 => Instruction::IntegerConstant(2),
			0x06 => Instruction::IntegerConstant(3),
			0x07 => Instruction::IntegerConstant(4),
			0x08 => Instruction::IntegerConstant(5),
			0x10 => Instruction::IntegerConstant(read_u8(code, &mut i) as u32),
			0x11 => Instruction::IntegerConstant(read_u16(code, &mut i) as u32),
			0x12 => Instruction::LoadConstant(read_u8(code, &mut i) as u16),
			0x15 => Instruction::Load(StackType::Integer, read_u8(code, &mut i)),
			0x1a => Instruction::Load(StackType::Integer, 0),
			0x1b => Instruction::Load(StackType::Integer, 1),
			0x1c => Instruction::Load(StackType::Integer, 2),
			0x1d => Instruction::Load(StackType::Integer, 3),
			0x2a => Instruction::Load(StackType::Reference, 0),
			0x2b => Instruction::Load(StackType::Reference, 1),
			0x2c => Instruction::Load(StackType::Reference, 2),
			0x2d => Instruction::Load(StackType::Reference, 3),
			0x36 => Instruction::Store(StackType::Integer, read_u8(code, &mut i)),
			0x3b => Instruction::Store(StackType::Integer, 0),
			0x3c => Instruction::Store(StackType::Integer, 1),
			0x3d => Instruction::Store(StackType::Integer, 2),
			0x3e => Instruction::Store(StackType::Integer, 3),
			0x4b => Instruction::Store(StackType::Reference, 0),
			0x4c => Instruction::Store(StackType::Reference, 1),
			0x4d => Instruction::Store(StackType::Reference, 2),
			0x4e => Instruction::Store(StackType::Reference, 3),
			0x59 => Instruction::Dup,
			0x60 => Instruction::Add(StackType::Integer),
			0x84 => Instruction::Increment(read_u8(code, &mut i), read_u8(code, &mut i) as u32),
			0x9f => Instruction::Compare(StackType::Integer, CompareType::EQ, relative_jump(ip, read_i16(code, &mut i))),
			0xa0 => Instruction::Compare(StackType::Integer, CompareType::NE, relative_jump(ip, read_i16(code, &mut i))),
			0xa1 => Instruction::Compare(StackType::Integer, CompareType::LT, relative_jump(ip, read_i16(code, &mut i))),
			0xa2 => Instruction::Compare(StackType::Integer, CompareType::GE, relative_jump(ip, read_i16(code, &mut i))),
			0xa3 => Instruction::Compare(StackType::Integer, CompareType::GT, relative_jump(ip, read_i16(code, &mut i))),
			0xa4 => Instruction::Compare(StackType::Integer, CompareType::LE, relative_jump(ip, read_i16(code, &mut i))),
			0xa5 => Instruction::Compare(StackType::Reference, CompareType::EQ, relative_jump(ip, read_i16(code, &mut i))),
			0xa6 => Instruction::Compare(StackType::Reference, CompareType::NE, relative_jump(ip, read_i16(code, &mut i))),
			0xa7 => Instruction::Goto(relative_jump(ip, read_i16(code, &mut i))),
			0xb7 => Instruction::InvokeSpecial(read_u16(code, &mut i)),
			0xb1 => Instruction::Return,
			0xb2 => Instruction::GetStatic(read_u16(code, &mut i)),
			0xb3 => Instruction::PutStatic(read_u16(code, &mut i)),
			0xb5 => Instruction::PutField(read_u16(code, &mut i)),
			0xb6 => Instruction::InvokeVirtual(read_u16(code, &mut i)),
			0xbb => Instruction::New(read_u16(code, &mut i)),
			0xbf => Instruction::Throw,
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
