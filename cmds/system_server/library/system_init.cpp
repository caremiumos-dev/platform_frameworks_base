/*
 * System server main initialization.
 *
 * The system server is responsible for becoming the Binder
 * context manager, supplying the root ServiceManager object
 * through which other services can be found.
 */

#define LOG_TAG "sysproc"

#include <androidfw/ResourceTypes.h>
#include <androidfw/StreamingZipInflater.h>
#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <utils/TextOutput.h>
#include <utils/Log.h>
#include <utils/ZipFileRO.h>

#include <SurfaceFlinger.h>
#include <SensorService.h>

#include <android_runtime/AndroidRuntime.h>

#include <dirent.h>
#include <signal.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <cutils/properties.h>
#include <private/android_filesystem_config.h>

using namespace android;

namespace android {
/**
 * This class is used to kill this process when the runtime dies.
 */
class GrimReaper : public IBinder::DeathRecipient {
public:
    GrimReaper() { }

    virtual void binderDied(const wp<IBinder>& who)
    {
        ALOGI("Grim Reaper killing system_server...");
        kill(getpid(), SIGKILL);
    }
};

} // namespace android

namespace {
    static const char* OVERLAY_DIR = "/vendor/overlay";
    static const char* IDMAP_BIN = "/system/bin/idmap";
    static const char* ORIG_APK = "/system/framework/framework-res.apk";
    static const char* TARGET_PACKAGE_NAME = "android";
    static const char* IDMAP_FILE_PREFIX = "/data/resource-cache/";
    static const char* IDMAP_FILE_SUFFIX = "@idmap";
#define NO_OVERLAY_TAG (-1000)

    void exec_idmap(const char* idmap_bin, const char* orig_apk,
            const char* overlay_apk, const char* idmap_file)
    {
        execl(idmap_bin, idmap_bin, "--path", orig_apk, overlay_apk, idmap_file, (char*)NULL);
        ALOGE("execl(%s) failed: %s\n", idmap_bin, strerror(errno));
    }

    String8 flatten_path(const char* path)
    {
        String16 tmp(path);
        tmp.replaceAll('/', '@');
        return String8(tmp);
    }

    int mkdir_p(const String8& path)
    {
        static const mode_t mode =
            S_IRUSR | S_IWUSR | S_IXUSR | S_IWGRP | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH;
        struct stat st;

        if (stat(path.string(), &st) == 0) {
            return 0;
        }
        if (mkdir_p(path.getPathDir()) < 0) {
            return -1;
        }
        if (mkdir(path.string(), 0755) != 0) {
            return -1;
        }
        if (chown(path.string(), AID_SYSTEM, AID_SYSTEM) == -1) {
            return -1;
        }
        if (chmod(path.string(), mode) == -1) {
            return -1;
        }
        return 0;
    }

    int create_idmap_symlink(const char* old_path, const char* orig_apk, int priority)
    {
        static const char* prefix = "/data/system/overlay";
        const String8 new_path =
            String8::format("%s/%s/%04d", prefix, flatten_path(orig_apk + 1).string(), priority);
        if (mkdir_p(new_path.getPathDir()) < 0) {
            return -1;
        }
        (void)unlink(new_path.string());
        return symlink(old_path, new_path.string());
    }

    int wait_idmap(pid_t pid)
    {
        int status;
        pid_t got_pid;

        while (1) {
            got_pid = waitpid(pid, &status, 0);
            if (got_pid == -1 && errno == EINTR) {
                continue;
            } else {
                break;
            }
        }
        if (got_pid != pid) {
            return -1;
        }
        if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
            return 0;
        } else {
            return status;
        }
    }

    int idmap(const char* idmap_bin, const char* orig_apk,
            const char* overlay_apk, const char* idmap_file, int priority)
    {
        int retval = -1;

        pid_t pid = fork();
        if (pid == 0) {
            // child
            exec_idmap(idmap_bin, orig_apk, overlay_apk, idmap_file);
            exit(1);
        } else {
            // parent
            retval = wait_idmap(pid);
        }

        return retval;
    }

    int parse_overlay_tag(const ResXMLTree& parser, const char* target_package_name)
    {
        const size_t N = parser.getAttributeCount();
        String16 target;
        int priority = -1;
        for (size_t i = 0; i < N; ++i) {
            size_t len;
            String16 key(parser.getAttributeName(i, &len));
            String16 value("");
            const uint16_t* p = parser.getAttributeStringValue(i, &len);
            if (p) {
                value = String16(p);
            }
            if (key == String16("target")) {
                target = value;
            } else if (key == String16("priority") && value.size() > 0) {
                priority = atoi(String8(value).string());
                if (priority < 0 || priority > 9999) {
                    return -2;
                }
            }
        }
        if (target == String16(target_package_name)) {
            return priority;
        }
        return NO_OVERLAY_TAG;
    }

    int parse_manifest(const void* data, size_t size, const char* target_package_name)
    {
        ResXMLTree parser(data, size);
        if (parser.getError() != NO_ERROR) {
            ALOGD("%s failed to init xml parser, error=0x%08x\n", __FUNCTION__, parser.getError());
            return -1;
        }

        ResXMLParser::event_code_t type;
        do {
            type = parser.next();
            if (type == ResXMLParser::START_TAG) {
                size_t len;
                String16 tag(parser.getElementName(&len));
                if (tag == String16("overlay")) {
                    return parse_overlay_tag(parser, target_package_name);
                }
            }
        } while (type != ResXMLParser::BAD_DOCUMENT && type != ResXMLParser::END_DOCUMENT);

        return NO_OVERLAY_TAG;
    }

    int parse_apk(const char* path, const char* target_package_name)
    {
        ZipFileRO zip;
        if (zip.open(path) != NO_ERROR) {
            ALOGW("%s: failed to open zip %s\n", __FUNCTION__, path);
            return -1;
        }
        ZipEntryRO entry;
        if ((entry = zip.findEntryByName("AndroidManifest.xml")) == NULL) {
            ALOGW("%s: failed to find entry AndroidManifest.xml\n", __FUNCTION__);
            return -2;
        }
        size_t uncompLen = 0;
        int method;
        if (!zip.getEntryInfo(entry, &method, &uncompLen, NULL, NULL, NULL, NULL)) {
            ALOGW("%s: failed to read entry info\n", __FUNCTION__);
            return -3;
        }
        // FIXME: ensure method == compressed?
        FileMap* dataMap = zip.createEntryFileMap(entry);
        if (!dataMap) {
            ALOGW("%s: failed to create FileMap\n", __FUNCTION__);
            return -4;
        }
        char* buf = new char[uncompLen];
        if (NULL == buf) {
            ALOGW("%s: failed to allocate %d byte\n", __FUNCTION__, uncompLen);
            return -5;
        }
        StreamingZipInflater inflater(dataMap, uncompLen);
        if (inflater.read(buf, uncompLen) < 0) {
            ALOGW("%s: failed to inflate %d byte\n", __FUNCTION__, uncompLen);
            delete[] buf;
            return -6;
        }

        int priority = parse_manifest(buf, uncompLen, target_package_name);
        delete[] buf;
        return priority;
    }

    int verify_system_idmap_file_is_up_to_date(const char* root_dir, const char* orig_apk,
            const char* target_package_name)
    {
        if (getuid() != AID_SYSTEM || getgid() != AID_SYSTEM) {
            // not user system -> nothing we can do
            ALOGE("Not user system: uid=%d, gid=%d\n", getuid(), getgid());
            return -1;
        }

        DIR* dir = opendir(root_dir);
        if (NULL == dir) {
            return -1;
        }

        struct dirent* dirent;
        while ((dirent = readdir(dir)) != NULL) {
            struct stat st;
            char path[PATH_MAX + 1];
            snprintf(path, PATH_MAX, "%s/%s", root_dir, dirent->d_name);
            if (stat(path, &st) < 0) {
                continue;
            }
            if (!S_ISREG(st.st_mode)) {
                continue;
            }

            int priority = parse_apk(path, target_package_name);
            if (priority < 0) {
                continue;
            }

            String8 idmap_path(IDMAP_FILE_PREFIX);
            idmap_path.append(flatten_path(path + 1));
            idmap_path.append(IDMAP_FILE_SUFFIX);
            if (idmap(IDMAP_BIN, orig_apk, path, idmap_path.string(), priority) < 0) {
                ALOGW("System server: failed to create idmap file %s for overlay %s\n",
                        idmap_path.string(), path);
                continue;
            }

            if (create_idmap_symlink(idmap_path.string(), orig_apk, priority) != 0) {
                ALOGW("System server: failed to create symlink for %s\n", idmap_path.string());
                (void)unlink(idmap_path.string());
                continue;
            }
        }

        closedir(dir);
        return 0;
    }
}

extern "C" status_t system_init()
{
    ALOGI("Entered system_init()");

    if (verify_system_idmap_file_is_up_to_date(OVERLAY_DIR, ORIG_APK, TARGET_PACKAGE_NAME) < 0) {
        ALOGI("System server: failed to verify idmap file(s) for framework-res.apk.\n");
    }

    sp<ProcessState> proc(ProcessState::self());

    sp<IServiceManager> sm = defaultServiceManager();
    ALOGI("ServiceManager: %p\n", sm.get());

    sp<GrimReaper> grim = new GrimReaper();
    sm->asBinder()->linkToDeath(grim, grim.get(), 0);

    char propBuf[PROPERTY_VALUE_MAX];
    property_get("system_init.startsurfaceflinger", propBuf, "1");
    if (strcmp(propBuf, "1") == 0) {
        // Start the SurfaceFlinger
        SurfaceFlinger::instantiate();
    }

    property_get("system_init.startsensorservice", propBuf, "1");
    if (strcmp(propBuf, "1") == 0) {
        // Start the sensor service
        SensorService::instantiate();
    }

    // And now start the Android runtime.  We have to do this bit
    // of nastiness because the Android runtime initialization requires
    // some of the core system services to already be started.
    // All other servers should just start the Android runtime at
    // the beginning of their processes's main(), before calling
    // the init function.
    ALOGI("System server: starting Android runtime.\n");
    AndroidRuntime* runtime = AndroidRuntime::getRuntime();

    ALOGI("System server: starting Android services.\n");
    JNIEnv* env = runtime->getJNIEnv();
    if (env == NULL) {
        return UNKNOWN_ERROR;
    }
    jclass clazz = env->FindClass("com/android/server/SystemServer");
    if (clazz == NULL) {
        return UNKNOWN_ERROR;
    }
    jmethodID methodId = env->GetStaticMethodID(clazz, "init2", "()V");
    if (methodId == NULL) {
        return UNKNOWN_ERROR;
    }
    env->CallStaticVoidMethod(clazz, methodId);

    ALOGI("System server: entering thread pool.\n");
    ProcessState::self()->startThreadPool();
    IPCThreadState::self()->joinThreadPool();
    ALOGI("System server: exiting thread pool.\n");

    return NO_ERROR;
}
