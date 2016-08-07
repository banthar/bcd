extern crate getopts;

mod class;
mod bytecode;
mod class_loader;

use getopts::Options;
use std::env;
use std::path::PathBuf;

use class_loader::ClassLoader;

fn print_usage(program: &str, opts: Options) {
    let brief = format!("Usage: {} [options] class", program);
    print!("{}", opts.usage(&brief));
}

fn main() {
  let mut opts = Options::new();
  opts.optopt("c", "classpath", "class search path", "PATH");
  opts.optflag("h", "help", "print this help menu");
  let args: Vec<String> = env::args().collect();
  let program = args[0].clone();
  let matches = match opts.parse(&args[1..]) {
      Ok(m) => { m }
      Err(f) => { panic!(f.to_string()) }
  };
  if matches.opt_present("h") {
      print_usage(&program, opts);
      return;
  }

	if matches.free.len() != 1 {
      println!("Expected exactly one class name");
      return;
	}

	let class_path = matches.opt_str("c").unwrap_or(".".to_owned()).split(':').map(PathBuf::from).collect();
	let mut class_loader = ClassLoader::new(class_path);
	let class = class_loader.load(&matches.free[0]).unwrap();

	for method in class.methods {
		match method.code {
			Some(code) => {
				let instructions = bytecode::parse_instructions(&code.instructions, &class.constant_pool, &code.stack_map);
				println!("{:?}", instructions);
			},
			None => {},
		}
	}
}
