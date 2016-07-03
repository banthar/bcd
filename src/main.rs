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
struct Method {
	access_flags: u16,
	name_index: u16,
	descriptor_index: u16,
	code: Option<Vec<u8>>
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

fn parse_method(input: &mut Read, constants: &Vec<Constant>) -> Result<Method, Box<Error>> {
	let access_flags = try!(input.read_u16::<BigEndian>());
	let name_index = try!(input.read_u16::<BigEndian>());
	let descriptor_index = try!(input.read_u16::<BigEndian>());
	let mut code = None;
	try!(parse_attributes(input, constants, |name, value| {
		return match name {
			"Code" => {
				let mut code_bytes = Vec::new();
				try!(value.read_to_end(&mut code_bytes));
				code = Some(code_bytes);
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
			delexer.token(&format!("{:?}", code));
			delexer.end_bracket();
		},
		None => delexer.token(";"),
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
