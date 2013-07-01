cmd_usr/initramfs_data.o := /source/aries-cm/kernel/xiaomi/aries/scripts/gcc-wrapper.py /source/aries-cm/prebuilts/misc/linux-x86/ccache/ccache /source/aries-cm/prebuilts/gcc/linux-x86/arm/arm-eabi-4.6/bin/arm-eabi-gcc -Wp,-MD,usr/.initramfs_data.o.d  -nostdinc -isystem /source/aries-cm/prebuilts/gcc/linux-x86/arm/arm-eabi-4.6/bin/../lib/gcc/arm-eabi/4.6.x-google/include -I/source/aries-cm/kernel/xiaomi/aries/arch/arm/include -Iarch/arm/include/generated -Iinclude  -I/source/aries-cm/kernel/xiaomi/aries/include -include /source/aries-cm/kernel/xiaomi/aries/include/linux/kconfig.h -D__KERNEL__ -mlittle-endian   -I/source/aries-cm/kernel/xiaomi/aries/arch/arm/mach-msm/include -D__ASSEMBLY__ -mabi=aapcs-linux -mno-thumb-interwork -funwind-tables -D__LINUX_ARM_ARCH__=7 -march=armv7-a -include asm/unified.h -msoft-float -gdwarf-2 -DINITRAMFS_IMAGE="usr/initramfs_data.cpio"   -c -o usr/initramfs_data.o /source/aries-cm/kernel/xiaomi/aries/usr/initramfs_data.S

source_usr/initramfs_data.o := /source/aries-cm/kernel/xiaomi/aries/usr/initramfs_data.S

deps_usr/initramfs_data.o := \
    $(wildcard include/config/64bit.h) \
  /source/aries-cm/kernel/xiaomi/aries/arch/arm/include/asm/unified.h \
    $(wildcard include/config/arm/asm/unified.h) \
    $(wildcard include/config/thumb2/kernel.h) \
  /source/aries-cm/kernel/xiaomi/aries/include/linux/stringify.h \
  /source/aries-cm/kernel/xiaomi/aries/include/asm-generic/vmlinux.lds.h \
    $(wildcard include/config/hotplug.h) \
    $(wildcard include/config/hotplug/cpu.h) \
    $(wildcard include/config/memory/hotplug.h) \
    $(wildcard include/config/ftrace/mcount/record.h) \
    $(wildcard include/config/trace/branch/profiling.h) \
    $(wildcard include/config/profile/all/branches.h) \
    $(wildcard include/config/event/tracing.h) \
    $(wildcard include/config/tracing.h) \
    $(wildcard include/config/ftrace/syscalls.h) \
    $(wildcard include/config/function/graph/tracer.h) \
    $(wildcard include/config/constructors.h) \
    $(wildcard include/config/generic/bug.h) \
    $(wildcard include/config/pm/trace.h) \
    $(wildcard include/config/blk/dev/initrd.h) \

usr/initramfs_data.o: $(deps_usr/initramfs_data.o)

$(deps_usr/initramfs_data.o):
