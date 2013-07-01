#ifndef _LINUX_KERNEL_H
#define _LINUX_KERNEL_H

#include <linux/sysinfo.h>

/*
 * 'kernel.h' contains some often-used function prototypes etc
 */
#define __ALIGN_KERNEL(x, a)		__ALIGN_KERNEL_MASK(x, (typeof(x))(a) - 1)
#define __ALIGN_KERNEL_MASK(x, mask)	(((x) + (mask)) & ~(mask))


/* To identify board information in panic logs, set this */
extern char *mach_panic_string;

int sysmat_writecmd(const char *cmdbuf, int cmdlen);

#endif
