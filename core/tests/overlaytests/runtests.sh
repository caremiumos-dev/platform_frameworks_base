#!/bin/bash

function usage()
{
	echo "Usage: runtest.sh [-e|-d|-s serial] [-0] [-1] [-2]"
	echo -e "\t-e  Target emulator (adb -e)"
	echo -e "\t-d  Target device (adb -d)"
	echo -e "\t-s serial  Target device with id <serial> (adb -s)"
	echo -e "\t-0  Execute the no overlay testsuite"
	echo -e "\t-1  Execute the single overlay testsuite"
	echo -e "\t-2  Execute the multiple overlay testsuite"
	echo "By default, all three testsuites are executed."
	echo "Any combination of -{0,1,2} may be given."
	echo "Example:"
	echo -e "\truntest.sh -e -01"
	echo "Run testsuites for no and single overlays on emulator."

}

adb="adb"
testsuites=""
while getopts "eds:012" opt; do
	case "$opt" in
		e)
			adb="adb -e"
			;;
		d)
			adb="adb -d"
			;;
		s)
			adb="adb -s ${OPTARG}"
			;;
		0)
			testsuites+="0"
			;;
		1)
			testsuites+="1"
			;;
		2)
			testsuites+="2"
			;;
		?)
			usage
			exit 1
			;;
	esac
done
if [[ -z "$testsuites" ]]; then testsuites="012"; fi

function atexit()
{
	local retval=$?

	if [[ $retval -eq 0 ]]; then
		rm $log
	else
		echo "There were errors, please check log at $log"
	fi
}

log=$(mktemp)
trap "atexit" EXIT

function compile_module()
{
	local android_mk="$1"

	echo "Compiling .${android_mk:${#PWD}}"
	ONE_SHOT_MAKEFILE="$android_mk" make -C "../../../../../" files | tee -a $log
	if [[ ${PIPESTATUS[0]} -ne 0 ]]; then
		exit 1
	fi
}

function wait_for_boot_completed()
{
	echo "Rebooting device"
	$adb wait-for-device logcat -c
	$adb wait-for-device logcat | grep -m 1 -e 'PowerManagerService.*bootCompleted' >/dev/null
}

function mkdir_if_needed()
{
	local path="$1"

	if [[ "${path:0:1}" != "/" ]]; then
		echo "mkdir_if_needed: error: path '$path' does not begin with /" | tee -a $log
		exit 1
	fi

	local basename=$(basename "$path")
	local dirname=$(dirname "$path")
	local t=$($adb shell ls -l $dirname | tr -d '\r' | grep -e "${basename}$" | grep -oe '^.')

	case "$t" in
		d) # File exists, and is a directory ...
			# do nothing
			;;
		l) # ... (or symbolic link possibly to a directory).
			# do nothing
			;;
		"") # File does not exist.
			mkdir_if_needed "$dirname"
			$adb shell mkdir "$path"
			;;
		*) # File exists, but is not a directory.
			echo "mkdir_if_needed: file '$path' exists, but is not a directory" | tee -a $log
			exit 1
			;;
	esac
}

function rm_if_needed()
{
	local path="$1"

	if [[ "${path:0:1}" != "/" ]]; then
		echo "rm_if_needed: error: path '$path' does not begin with /" | tee -a $log
		exit 1
	fi
	local t="$($adb shell ls $path | tr -d '\r' | grep -v 'No such file or directory')"

	if [[ "$t" ]]; then
		$adb shell rm -r "$path"
	fi
}

function disable_overlay()
{
	echo "Disabling all overlays"

	rm_if_needed "/vendor/overlay/framework_a.apk"
	rm_if_needed "/vendor/overlay/framework_b.apk"
	rm_if_needed "/data/resource-cache/vendor@overlay@framework_a.apk@idmap"
	rm_if_needed "/data/resource-cache/vendor@overlay@framework_b.apk@idmap"

	rm_if_needed "/vendor/overlay/app_a.apk"
	rm_if_needed "/vendor/overlay/app_b.apk"
	rm_if_needed "/data/resource-cache/vendor@overlay@app_a.apk@idmap"
	rm_if_needed "/data/resource-cache/vendor@overlay@app_b.apk@idmap"

	rm_if_needed "/data/system/overlay"
}

function enable_overlay()
{
	disable_overlay
	echo "Enabling single overlay"

	mkdir_if_needed "/system/vendor"
	mkdir_if_needed "/vendor/overlay"

	push "/data/app/com.android.overlaytest.overlay.apk" "/vendor/overlay/framework_a.apk"
	push "/data/app/com.android.overlaytest.first_app_overlay.apk" "/vendor/overlay/app_a.apk"
}

function enable_multiple_overlays()
{
	disable_overlay
	echo "Enabling multiple overlays"

	mkdir_if_needed "/system/vendor"
	mkdir_if_needed "/vendor/overlay"

	# The order in which overlay packages (targeting the same base package) are loaded is
	# determined by the overlay packages' <overlay priority=..."/> fields. The actual file
	# names should not matter: for framework-res.apk, explicitly name overlay packages in
	# reverse lexicographical order to verify this.
	push "/data/app/com.android.overlaytest.overlay.apk" "/vendor/overlay/framework_b.apk"
	push "/data/app/com.android.overlaytest.multipleoverlays.apk" "/vendor/overlay/framework_a.apk"
	push "/data/app/com.android.overlaytest.first_app_overlay.apk" "/vendor/overlay/app_a.apk"
	push "/data/app/com.android.overlaytest.second_app_overlay.apk" "/vendor/overlay/app_b.apk"
}

function instrument()
{
	local class="$1"

	echo "Instrumenting $class"
	$adb shell am instrument -w -e class $class com.android.overlaytest/android.test.InstrumentationTestRunner | tee -a $log
}

function remount()
{
	echo "Remounting file system writable"
	$adb remount | tee -a $log
}

function push()
{
	local src=$OUT/$1
	local dest=$2
	echo "Pushing $1 -> $2" | tee -a $log
	$adb push $src $dest
}

# paths below are given relative to this script
cd $(dirname $(readlink -f "$0"))

# some commands require write access, remount once and for all
remount

# build and push
compile_module "$PWD/OverlayTest/Android.mk"
compile_module "$PWD/OverlayTestOverlay/Android.mk"
compile_module "$PWD/OverlayTestMultipleOverlays/Android.mk"
compile_module "$PWD/OverlayAppFirst/Android.mk"
compile_module "$PWD/OverlayAppSecond/Android.mk"
push "/system/app/OverlayTest.apk" "/system/app/OverlayTest.apk"
push "/system/app/OverlayTest.odex" "/system/app/OverlayTest.odex"

# instrument test (without overlay)
if [[ "$testsuites" =~ "0" ]]; then
	$adb shell stop
	disable_overlay
	$adb shell start
	wait_for_boot_completed
	instrument "com.android.overlaytest.WithoutOverlayTest"
fi

# instrument test (with overlay)
if [[ "$testsuites" =~ "1" ]]; then
	$adb shell stop
	enable_overlay
	$adb shell start
	wait_for_boot_completed
	instrument "com.android.overlaytest.WithOverlayTest"
fi

# instrument test (with multiple overlays)
if [[ "$testsuites" =~ "2" ]]; then
	$adb shell stop
	enable_multiple_overlays
	$adb shell start
	wait_for_boot_completed
	instrument "com.android.overlaytest.WithMultipleOverlaysTest"
fi

# cleanup
exit $(grep -c -e '^FAILURES' $log)
