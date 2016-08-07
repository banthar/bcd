
use std::path::PathBuf;
use std::path::Path;
use class::ClassFile;
use std::error::Error;
use std::fs::File;
use std::io::ErrorKind;

pub struct ClassLoader {
	class_path:Vec<PathBuf>,
}

impl ClassLoader {
	pub fn new(class_path: Vec<PathBuf>) -> ClassLoader {
		return ClassLoader {
			class_path: class_path,
		}
	}

	pub fn load(&mut self, class: &str) -> Result<ClassFile, Box<Error>> {
		let relative_path_name = class.replace(".", "/")+".class";
		let relative_path = Path::new(&relative_path_name);
		for prefix in &self.class_path {
			let mut file = match File::open(prefix.as_path().join(relative_path)) {
				Ok(f) => f,
				Err(e) => {
					if e.kind() == ErrorKind::NotFound {
						continue
					} else {
						return Err(Box::new(e))
					}
				}
			};
			return ClassFile::read(&mut file)
		}
		panic!("Class not found")
	}
}
