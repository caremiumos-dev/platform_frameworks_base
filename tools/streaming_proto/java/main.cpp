#include <stdio.h>

#include <iomanip>
#include <iostream>
#include <map>
#include <sstream>
#include <string>

#include "Errors.h"
#include "stream_proto_utils.h"
#include "string_utils.h"

using namespace android::stream_proto;
using namespace google::protobuf::io;
using namespace std;

/**
 * If the descriptor gives us a class name, use that. Otherwise make one up from
 * the filename of the .proto file.
 */
static string
make_outer_class_name(const FileDescriptorProto& file_descriptor)
{
    string name = file_descriptor.options().java_outer_classname();
    if (name.size() == 0) {
        name = to_camel_case(file_base_name(file_descriptor.name()));
        if (name.size() == 0) {
            ERRORS.Add(UNKNOWN_FILE, UNKNOWN_LINE,
                    "Unable to make an outer class name for file: %s",
                    file_descriptor.name().c_str());
            name = "Unknown";
        }
    }
    return name;
}

/**
 * Figure out the package name that we are generating.
 */
static string
make_java_package(const FileDescriptorProto& file_descriptor) {
    if (file_descriptor.options().has_java_package()) {
        return file_descriptor.options().java_package();
    } else {
        return file_descriptor.package();
    }
}

/**
 * Figure out the name of the file we are generating.
 */
static string
make_file_name(const FileDescriptorProto& file_descriptor, const string& class_name)
{
    string const package = make_java_package(file_descriptor);
    string result;
    if (package.size() > 0) {
        result = replace_string(package, '.', '/');
        result += '/';
    }

    result += class_name;
    result += ".java";

    return result;
}

static string
indent_more(const string& indent)
{
    return indent + INDENT;
}

/**
 * Write the constants for an enum.
 */
static void
write_enum(stringstream& text, const EnumDescriptorProto& enu, const string& indent)
{
    const int N = enu.value_size();
    text << indent << "// enum " << enu.name() << endl;
    for (int i=0; i<N; i++) {
        const EnumValueDescriptorProto& value = enu.value(i);
        text << indent << "public static final int "
                << make_constant_name(value.name())
                << " = " << value.number() << ";" << endl;
    }
    text << endl;
}

/**
 * Write the constants for an enum.
 */
static void no_class_write_enum(stringstream& text, string prefix, const EnumDescriptorProto& enu,
                                const string& indent) {
    const int N = enu.value_size();
    text << indent << "// enum " << enu.name() << endl;
    for (int i = 0; i < N; i++) {
        const EnumValueDescriptorProto& value = enu.value(i);
        text << indent << "public static final int " << make_constant_name(prefix + value.name())
             << " = " << value.number() << ";" << endl;
    }
    text << endl;
}

/**
 * Write a field.
 */
static void
write_field(stringstream& text, const FieldDescriptorProto& field, const string& indent)
{
    string optional_comment = field.label() == FieldDescriptorProto::LABEL_OPTIONAL
            ? "optional " : "";
    string repeated_comment = field.label() == FieldDescriptorProto::LABEL_REPEATED
            ? "repeated " : "";
    string proto_type = get_proto_type(field);
    string packed_comment = field.options().packed()
            ? " [packed=true]" : "";
    text << indent << "// " << optional_comment << repeated_comment << proto_type << ' '
            << field.name() << " = " << field.number() << packed_comment << ';' << endl;

    text << indent << "public static final long " << make_constant_name(field.name()) << " = 0x";

    ios::fmtflags fmt(text.flags());
    text << setfill('0') << setw(16) << hex << get_field_id(field);
    text.flags(fmt);

    text << "L;" << endl;

    text << endl;
}

/**
 * Write a field.
 */
static void no_class_write_field(stringstream& text, string prefix,
                                 const FieldDescriptorProto& field, const string& indent) {
    string optional_comment =
            field.label() == FieldDescriptorProto::LABEL_OPTIONAL ? "optional " : "";
    string repeated_comment =
            field.label() == FieldDescriptorProto::LABEL_REPEATED ? "repeated " : "";
    string proto_type = get_proto_type(field);
    string packed_comment = field.options().packed() ? " [packed=true]" : "";
    text << indent << "// " << optional_comment << repeated_comment << proto_type << ' '
         << field.name() << " = " << field.number() << packed_comment << ';' << endl;

    text << indent << "public static final long " << make_constant_name(prefix + field.name())
         << " = 0x";

    ios::fmtflags fmt(text.flags());
    text << setfill('0') << setw(16) << hex << get_field_id(field);
    text.flags(fmt);

    text << "L;" << endl;

    text << endl;
}

/**
 * Write a Message constants class.
 */
static void
write_message(stringstream& text, const DescriptorProto& message, const string& indent)
{
    int N;
    const string indented = indent_more(indent);

    text << indent << "// message " << message.name() << endl;
    text << indent << "public final class " << message.name() << " {" << endl;
    text << endl;

    // Enums
    N = message.enum_type_size();
    for (int i=0; i<N; i++) {
        write_enum(text, message.enum_type(i), indented);
    }

    // Nested classes
    N = message.nested_type_size();
    for (int i=0; i<N; i++) {
        write_message(text, message.nested_type(i), indented);
    }

    // Fields
    N = message.field_size();
    for (int i=0; i<N; i++) {
        write_field(text, message.field(i), indented);
    }

    text << indent << "}" << endl;
    text << endl;
}

/**
 * Write a Message constants class.
 */
static void no_class_write_message(stringstream& text, string prefix,
                                   const DescriptorProto& message, const string& indent) {
    int N;
    const string indented = indent_more(indent);

    text << indent << "// message " << message.name() << endl;

    prefix = prefix + "_" + message.name();

    // Enums
    N = message.enum_type_size();
    for (int i = 0; i < N; i++) {
        no_class_write_enum(text, prefix, message.enum_type(i), indented);
    }

    // Nested classes
    N = message.nested_type_size();
    for (int i = 0; i < N; i++) {
        no_class_write_message(text, prefix, message.nested_type(i), indented);
    }

    // Fields
    N = message.field_size();
    for (int i = 0; i < N; i++) {
        no_class_write_field(text, prefix, message.field(i), indented);
    }

    text << endl;
}

/**
 * Write the contents of a file.
 *
 * If there are enums and generate_outer is false, invalid java code will be generated.
 */
static void
write_file(CodeGeneratorResponse* response, const FileDescriptorProto& file_descriptor,
        const string& filename, bool generate_outer,
        const vector<EnumDescriptorProto>& enums, const vector<DescriptorProto>& messages)
{
    stringstream text;

    string const package_name = make_java_package(file_descriptor);
    string const outer_class_name = make_outer_class_name(file_descriptor);

    text << "// Generated by protoc-gen-javastream. DO NOT MODIFY." << endl;
    text << "// source: " << file_descriptor.name() << endl << endl;

    if (package_name.size() > 0) {
        if (package_name.size() > 0) {
            text << "package " << package_name << ";" << endl;
            text << endl;
        }
    }

    // This bit of policy is android api rules specific: Raw proto classes
    // must never be in the API
    text << "/** @hide */" << endl;
//    text << "@android.annotation.TestApi" << endl;

    if (generate_outer) {
        text << "public final class " << outer_class_name << " {" << endl;
        text << endl;
    }

    size_t N;
    const string indented = generate_outer ? indent_more("") : string();
    
    N = enums.size();
    for (size_t i=0; i<N; i++) {
        write_enum(text, enums[i], indented);
    }

    N = messages.size();
    for (size_t i=0; i<N; i++) {
        write_message(text, messages[i], indented);
    }

    if (generate_outer) {
        text << "}" << endl;
    }

    CodeGeneratorResponse::File* file_response = response->add_file();
    file_response->set_name(filename);
    file_response->set_content(text.str());
}

/**
 * Write the contents of a file.
 *
 * If there are enums and generate_outer is false, invalid java code will be generated.
 */
static void no_class_write_file(CodeGeneratorResponse* response,
                                const FileDescriptorProto& file_descriptor, const string& filename,
                                bool generate_outer, const vector<EnumDescriptorProto>& enums,
                                const vector<DescriptorProto>& messages) {
    stringstream text;

    string const package_name = make_java_package(file_descriptor);
    string const outer_class_name = make_outer_class_name(file_descriptor);

    text << "// Generated by protoc-gen-javastream. DO NOT MODIFY." << endl;
    text << "// source: " << file_descriptor.name() << endl << endl;

    if (package_name.size() > 0) {
        if (package_name.size() > 0) {
            text << "package " << package_name << ";" << endl;
            text << endl;
        }
    }

    // This bit of policy is android api rules specific: Raw proto classes
    // must never be in the API
    text << "/** @hide */" << endl;
    //    text << "@android.annotation.TestApi" << endl;

    size_t N;
    const string indented = generate_outer ? indent_more("") : string();

    N = enums.size();
    for (size_t i = 0; i < N; i++) {
        no_class_write_enum(text, outer_class_name, enums[i], indented);
    }

    N = messages.size();
    for (size_t i = 0; i < N; i++) {
        no_class_write_message(text, outer_class_name, messages[i], indented);
    }

    CodeGeneratorResponse::File* file_response = response->add_file();
    file_response->set_name(filename);
    file_response->set_content(text.str());
}

/**
 * Write one file per class.  Put all of the enums into the "outer" class.
 */
static void
write_multiple_files(CodeGeneratorResponse* response, const FileDescriptorProto& file_descriptor)
{
    // If there is anything to put in the outer class file, create one
    if (file_descriptor.enum_type_size() > 0) {
        vector<EnumDescriptorProto> enums;
        int N = file_descriptor.enum_type_size();
        for (int i=0; i<N; i++) {
            enums.push_back(file_descriptor.enum_type(i));
        }

        vector<DescriptorProto> messages;

        write_file(response, file_descriptor,
                make_file_name(file_descriptor, make_outer_class_name(file_descriptor)),
                true, enums, messages);
    }

    // For each of the message types, make a file
    int N = file_descriptor.message_type_size();
    for (int i=0; i<N; i++) {
        vector<EnumDescriptorProto> enums;

        vector<DescriptorProto> messages;
        messages.push_back(file_descriptor.message_type(i));

        write_file(response, file_descriptor,
                make_file_name(file_descriptor, file_descriptor.message_type(i).name()),
                false, enums, messages);
    }
}

static void write_single_file(CodeGeneratorResponse* response,
                              const FileDescriptorProto& file_descriptor,
                              set<string> messages_to_compile) {
    int N;

    vector<EnumDescriptorProto> enums;
    vector<EnumDescriptorProto> no_class_enums;
    N = file_descriptor.enum_type_size();
    for (int i=0; i<N; i++) {
        auto message_full_name =
                file_descriptor.package() + "." + file_descriptor.enum_type(i).name();
        if (!messages_to_compile.empty() && !messages_to_compile.count(message_full_name)) {
            if (file_descriptor.name().find("perfetto") != string::npos) {
                // A Perfetto proto file
                no_class_enums.push_back(file_descriptor.enum_type(i));
            }
            continue;
        }

        enums.push_back(file_descriptor.enum_type(i));
    }

    vector<DescriptorProto> messages;
    vector<DescriptorProto> no_class_messages;
    N = file_descriptor.message_type_size();
    for (int i=0; i<N; i++) {
        auto message_full_name =
                file_descriptor.package() + "." + file_descriptor.message_type(i).name();

        if (!messages_to_compile.empty() && !messages_to_compile.count(message_full_name)) {
            if (file_descriptor.name().find("perfetto") != string::npos) {
                // A Perfetto proto file
                no_class_messages.push_back(file_descriptor.message_type(i));
            }
            continue;
        }

        messages.push_back(file_descriptor.message_type(i));
    }

    write_file(response, file_descriptor,
            make_file_name(file_descriptor, make_outer_class_name(file_descriptor)),
            true, enums, messages);

    no_class_write_file(response, file_descriptor,
                        make_file_name(file_descriptor,
                                       make_outer_class_name(file_descriptor) + "_NO_CLASS"),
                        true, no_class_enums, no_class_messages);
}

static void parse_args_string(stringstream args_string_stream,
                              set<string>* messages_to_compile_out) {
    string line;
    while (getline(args_string_stream, line, ';')) {
        stringstream line_ss(line);
        string arg_name;
        getline(line_ss, arg_name, ':');
        if (arg_name == "include_filter") {
            string full_message_name;
            while (getline(line_ss, full_message_name, ',')) {
                messages_to_compile_out->insert(full_message_name);
            }
        } else {
            ERRORS.Add(UNKNOWN_FILE, UNKNOWN_LINE, "Unexpected argument '%s'.", arg_name.c_str());
        }
    }
}

/**
 * Main.
 */
int
main(int argc, char const*const* argv)
{
    (void)argc;
    (void)argv;

    GOOGLE_PROTOBUF_VERIFY_VERSION;

    CodeGeneratorRequest request;
    CodeGeneratorResponse response;

    // Read the request
    request.ParseFromIstream(&cin);

    set<string> messages_to_compile;
    auto request_params = request.parameter();
    if (!request_params.empty()) {
        parse_args_string(stringstream(request_params), &messages_to_compile);
    }

    // Build the files we need.
    const int N = request.proto_file_size();
    for (int i=0; i<N; i++) {
        const FileDescriptorProto& file_descriptor = request.proto_file(i);
        if (should_generate_for_file(request, file_descriptor.name())) {
            if (file_descriptor.options().java_multiple_files()) {
                write_multiple_files(&response, file_descriptor);
            } else {
                write_single_file(&response, file_descriptor, messages_to_compile);
            }
        }
    }

    // If we had errors, don't write the response. Print the errors and exit.
    if (ERRORS.HasErrors()) {
        ERRORS.Print();
        return 1;
    }

    // If we didn't have errors, write the response and exit happily.
    response.SerializeToOstream(&cout);
    return 0;
}
