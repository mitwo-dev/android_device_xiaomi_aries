/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "syspart_select"
#include <utils/Log.h>
#include <sys/stat.h>
#include <sys/mount.h>
#include <cutils/klog.h>
#include <sys/system_properties.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>

#define INFO(x...)    KLOG_INFO(LOG_TAG, x)
#define ERROR(x...)   KLOG_ERROR(LOG_TAG, x)

#define PATH_BY_NAME "/dev/block/platform/msm_sdcc.1/by-name/"
#define PATH_BACKUP  "/dev/block_backup/"

#define PARTITION_SYSTEM "system"
#define PARTITION_SYSTEM1 "system1"
#define PARTITION_BOOT "boot"
#define PARTITION_BOOT1 "boot1"
#define PARTITION_MODEM "modem"
#define PARTITION_MODEM1 "modem1"

static int set_bootmode(int mode) {
	const char* strmode;

	if(mode) strmode = "boot-system1";
	else strmode = "boot-system";

	// open misc-partition
	FILE* misc = fopen("/dev/block/platform/msm_sdcc.1/by-name/misc", "wb");
	if (misc == NULL) {
		ERROR("Error opening misc partition.\n");
		return -1;
	}

	// write bootmode
	fseek(misc, 0x1000, SEEK_SET);
	if(fputs(strmode, misc)<0 || fputc('\0', misc)<0) {
		ERROR("Error writing bootmode to misc partition.\n");
		return -1;
	}

	// close
	fclose(misc);
	return 0;
}

static int get_bootmode(void) {
	int rc = 0;
	char bootmode[13];

	// open misc-partition
	FILE* misc = fopen("/dev/block/platform/msm_sdcc.1/by-name/misc", "rb");
	if (misc == NULL) {
		ERROR("Error opening misc partition.\n");
		return -1;
	}

	// read bootmode
	fseek(misc, 0x1000, SEEK_SET);
	if(fgets(bootmode, 13, misc)==NULL) {
		ERROR("Error reading bootmode from misc partition.\n");
		return -1;
	}

	if(!strncmp(bootmode, "boot-system1", 13))
		rc = 1;

	// close
	fclose(misc);

	return rc;
}

static int backupPart(const char* name) {
	struct stat sb;
	char path_source[PATH_MAX], path_backup[PATH_MAX];

	snprintf(path_source, PATH_MAX, PATH_BY_NAME "%s", name);
	snprintf(path_backup, PATH_MAX, PATH_BACKUP "%s", name);

	// backup already exists
	if(!stat(path_backup, &sb)) {
		INFO("%s already exists!\n", path_backup);
		return 0;
	}

	// backup already exists
	if(stat(path_source, &sb)) {
		ERROR("Could not stat %s!\n", path_source);
		return -1;
	}

	// create backup
	if(mknod(path_backup, sb.st_mode, sb.st_rdev)!=0) {
		ERROR("could not create %s!\n", path_backup);
		return -1;
	}

	return 0;
}

static int createPart(const char* name, int syspart) {
	struct stat sb;
	char path_part[PATH_MAX], path_part1[PATH_MAX], path_backup[PATH_MAX];
	char resolved_part[PATH_MAX], resolved_part1[PATH_MAX];

	snprintf(path_part, PATH_MAX, PATH_BY_NAME "%s", name);
	snprintf(path_part1, PATH_MAX, PATH_BY_NAME "%s1", name);
	snprintf(path_backup, PATH_MAX, PATH_BACKUP "%s%s", name, syspart?"1":"");

	if(!realpath(path_part, resolved_part)) {
		ERROR("Error resolving %s\n", path_part);
		return -1;
	}
	if(!realpath(path_part1, resolved_part1)) {
		ERROR("Error resolving %s\n", path_part1);
		return -1;
	}

	// stat backup part
	if(stat(path_backup, &sb)) {
		ERROR("could not stat %s!\n", path_backup);
		return -1;
	}

	// remove old node
	unlink(resolved_part);

	// create new node
	if(mknod(resolved_part, sb.st_mode, sb.st_rdev)!=0) {
		ERROR("could not create %s!\n", resolved_part);
		return -1;
	}

	// remove sys1 node
	unlink(resolved_part1);
	if(mknod(resolved_part1, sb.st_mode, makedev(0,0))!=0) {
		ERROR("could not create %s!\n", resolved_part1);
		return -1;
	}

	return 0;
}

int main(int argc, char **argv)
{
	int ret=0, syspart;
	char filenamePatched[PATH_MAX];

	// check arguments
	if(argc!=2) {
		ERROR("Invalid Arguments\n");
		return -EINVAL;
	}

	// get syspart from cmdline
	syspart = get_bootmode();
	if(syspart<0) {
		ERROR("Cannot read system number\n");
		return -EINVAL;
	}

	char* mode = argv[1];
	if(!strcmp(mode, "auto")) {
		// nothing todo
	}
	else if(!strcmp(mode, "switch")) {
		syspart = !syspart;
	}
	else {
		ERROR("Invalid Argument '%s'\n", mode);
        return -EINVAL;
	}

	INFO("Selecting system%d\n", syspart);

	// backup nodes
	mkdir(PATH_BACKUP, 0755);
	backupPart(PARTITION_SYSTEM);
	backupPart(PARTITION_SYSTEM1);
	backupPart(PARTITION_BOOT);
	backupPart(PARTITION_BOOT1);
	backupPart(PARTITION_MODEM);
	backupPart(PARTITION_MODEM1);

	// unmount
	umount(PATH_BY_NAME PARTITION_SYSTEM);
	umount(PATH_BY_NAME PARTITION_SYSTEM1);
	umount(PATH_BY_NAME PARTITION_BOOT);
	umount(PATH_BY_NAME PARTITION_BOOT1);
	umount(PATH_BY_NAME PARTITION_MODEM);
	umount(PATH_BY_NAME PARTITION_MODEM1);

	// create new nodes
	createPart(PARTITION_SYSTEM, syspart);
	createPart(PARTITION_BOOT, syspart);
	createPart(PARTITION_MODEM, syspart);

	set_bootmode(syspart);

	return ret;
}
