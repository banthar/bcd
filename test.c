typedef long long ssize_t;
typedef unsigned long long size_t;

__attribute__ ((noreturn))
static void exit(int code) {
	asm volatile("syscall" : : "a" (60), "D" (code));
	__builtin_unreachable();
}

static ssize_t read(int fd, void *buf, size_t count) {
	int ret;
	asm volatile("syscall" : "=a" (ret) : "a" (0), "D" (fd), "S" (buf), "d" (count));
	return ret;
}

static ssize_t write(int fd, const void *buf, size_t count) {
	int ret;
	asm volatile("syscall" : "=a" (ret) : "a" (1), "D" (fd), "S" (buf), "d" (count));
	return ret;
}

int _start() {
	int n = 1;
	write(1, &n, 4);
	exit(0);
}

