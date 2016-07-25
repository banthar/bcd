use std::error::Error;
use std::env;
use std::fs::File;

mod class;
mod bytecode;

fn parse_class_file(file_name: &str) -> Result<class::ClassFile, Box<Error>> {
	let mut file = try!(File::open(file_name));
	class::ClassFile::read(&mut file)
}

fn main() {
	for arg in env::args().skip(1) {
		match  parse_class_file(&arg) {
			Err(e) => {println!("{}", e); break;},
			Ok(class) => {
				for method in class.methods {
					match method.code {
						Some(code) => {
							let instructions = bytecode::parse_instructions(&code.instructions, &class.constant_pool, &code.stack_map);
							println!("{:?}", instructions);
						},
						None => {},
					}
				}
			},
		}
	}
}
