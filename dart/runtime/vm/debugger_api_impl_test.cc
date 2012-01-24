// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "include/dart_debugger_api.h"
#include "platform/assert.h"
#include "vm/dart_api_impl.h"
#include "vm/unit_test.h"

namespace dart {

// Only ia32 and x64 can run execution tests.
#if defined(TARGET_ARCH_IA32) || defined(TARGET_ARCH_X64)

static bool breakpoint_hit = false;
static int  breakpoint_hit_counter = 0;

static const bool verbose = false;

#define EXPECT_NOT_ERROR(handle)                                              \
  if (Dart_IsError(handle)) {                                                 \
    OS::Print("Error: %s\n", Dart_GetError(handle));                          \
  }                                                                           \
  EXPECT(!Dart_IsError(handle));


void TestBreakpointHandler(Dart_Breakpoint bpt, Dart_StackTrace trace) {
  const char* expected_trace[] = {"A.foo", "main"};
  const intptr_t expected_trace_length = 2;
  breakpoint_hit = true;
  breakpoint_hit_counter++;
  intptr_t trace_len;
  Dart_Handle res = Dart_StackTraceLength(trace, &trace_len);
  EXPECT_NOT_ERROR(res);
  EXPECT_EQ(expected_trace_length, trace_len);
  for (int i = 0; i < trace_len; i++) {
    Dart_ActivationFrame frame;
    res = Dart_GetActivationFrame(trace, i, &frame);
    EXPECT_NOT_ERROR(res);
    Dart_Handle func_name;
    res = Dart_ActivationFrameInfo(frame, &func_name, NULL, NULL);
    EXPECT_NOT_ERROR(res);
    EXPECT(Dart_IsString(func_name));
    const char* name_chars;
    Dart_StringToCString(func_name, &name_chars);
    EXPECT_STREQ(expected_trace[i], name_chars);
    if (verbose) printf("  >> %d: %s\n", i, name_chars);
  }
}


TEST_CASE(Debug_Breakpoint) {
  const char* kScriptChars =
      "void moo(s) { }\n"
      "class A {\n"
      "  static void foo() {\n"
      "    moo('good news');\n"
      "  }\n"
      "}\n"
      "void main() {\n"
      "  A.foo();\n"
      "}\n";

  Dart_Handle lib = TestCase::LoadTestScript(kScriptChars, NULL);
  EXPECT(!Dart_IsError(lib));

  Dart_SetBreakpointHandler(&TestBreakpointHandler);

  Dart_Handle c_name = Dart_NewString("A");
  Dart_Handle f_name = Dart_NewString("foo");
  Dart_Breakpoint bpt;
  Dart_Handle res = Dart_SetBreakpointAtEntry(lib, c_name, f_name, &bpt);
  EXPECT_NOT_ERROR(res);

  breakpoint_hit = false;
  Dart_Handle retval = Dart_InvokeStatic(lib,
                           Dart_NewString(""),
                           Dart_NewString("main"),
                           0,
                           NULL);
  EXPECT(!Dart_IsError(retval));
  EXPECT(breakpoint_hit == true);
}


static void DeleteBreakpointHandler(Dart_Breakpoint bpt,
                                    Dart_StackTrace trace) {
  const char* expected_trace[] = {"foo", "main"};
  const intptr_t expected_trace_length = 2;
  breakpoint_hit_counter++;
  intptr_t trace_len;
  Dart_Handle res = Dart_StackTraceLength(trace, &trace_len);
  EXPECT_NOT_ERROR(res);
  EXPECT_EQ(expected_trace_length, trace_len);
  for (int i = 0; i < trace_len; i++) {
    Dart_ActivationFrame frame;
    res = Dart_GetActivationFrame(trace, i, &frame);
    EXPECT_NOT_ERROR(res);
    Dart_Handle func_name;
    res = Dart_ActivationFrameInfo(frame, &func_name, NULL, NULL);
    EXPECT_NOT_ERROR(res);
    EXPECT(Dart_IsString(func_name));
    const char* name_chars;
    Dart_StringToCString(func_name, &name_chars);
    EXPECT_STREQ(expected_trace[i], name_chars);
    if (verbose) printf("  >> %d: %s\n", i, name_chars);
  }
  // Remove the breakpoint after we've hit it twice
  if (breakpoint_hit_counter == 2) {
    if (verbose) printf("uninstalling breakpoint\n");
    Dart_Handle res = Dart_DeleteBreakpoint(bpt);
    EXPECT_NOT_ERROR(res);
  }
}


TEST_CASE(Debug_DeleteBreakpoint) {
  const char* kScriptChars =
      "moo(s) { }\n"
      "\n"
      "foo() {\n"
      "    moo('good news');\n"
      "}\n"
      "\n"
      "void main() {\n"
      "  foo();\n"
      "  foo();\n"
      "  foo();\n"
      "}\n";

  Dart_Handle lib = TestCase::LoadTestScript(kScriptChars, NULL);
  EXPECT(!Dart_IsError(lib));

  Dart_Handle script_url = Dart_NewString(TestCase::url());
  Dart_Handle line_no = Dart_NewInteger(4);  // In function 'foo'.

  Dart_SetBreakpointHandler(&DeleteBreakpointHandler);

  Dart_Breakpoint bpt;
  Dart_Handle res = Dart_SetBreakpointAtLine(script_url, line_no, &bpt);
  EXPECT_NOT_ERROR(res);

  // Function main() calls foo() 3 times. On the second iteration, the
  // breakpoint is removed by the handler, so we expect the breakpoint
  // to fire twice only.
  breakpoint_hit_counter = 0;
  Dart_Handle retval = Dart_InvokeStatic(lib,
                                         Dart_NewString(""),
                                         Dart_NewString("main"),
                                         0,
                                         NULL);
  EXPECT(!Dart_IsError(retval));
  EXPECT(breakpoint_hit_counter == 2);
}


TEST_CASE(Debug_LookupSourceLine) {
  const char* kScriptChars =
  /*1*/  "class A {\n"
  /*2*/  "  static void foo() {\n"
  /*3*/  "    moo('good news');\n"
  /*4*/  "  }\n"
  /*5*/  "}\n"
  /*6*/  "\n"
  /*7*/  "void main() {\n"
  /*8*/  "  A.foo();\n"
  /*9*/  "}\n"
  /*10*/ "\n";

  Dart_Handle lib = TestCase::LoadTestScript(kScriptChars, NULL);
  EXPECT(!Dart_IsError(lib));

  const Library& test_lib = Library::CheckedHandle(Api::UnwrapHandle(lib));
  const String& script_url = String::Handle(String::New(TestCase::url()));
  Function& func = Function::Handle();

  // TODO(hausner): Looking up functions from source and line number
  // needs to be refined. We currently dont find "main" on line 7.
  for (int line = 8; line <= 9; line++) {
    func = test_lib.LookupFunctionInSource(script_url, line);
    EXPECT(!func.IsNull());
    EXPECT_STREQ("main", String::Handle(func.name()).ToCString());
  }

  func = test_lib.LookupFunctionInSource(script_url, 3);
  EXPECT(!func.IsNull());
  EXPECT_STREQ("foo", String::Handle(func.name()).ToCString());

  func = test_lib.LookupFunctionInSource(script_url, 1);
  EXPECT(func.IsNull());
  func = test_lib.LookupFunctionInSource(script_url, 6);
  EXPECT(func.IsNull());
  func = test_lib.LookupFunctionInSource(script_url, 10);
  EXPECT(func.IsNull());

  Dart_Handle libs = Dart_GetLibraryURLs();
  EXPECT(Dart_IsList(libs));
  intptr_t num_libs;
  Dart_ListLength(libs, &num_libs);
  EXPECT(num_libs > 0);
  for (intptr_t i = 0; i < num_libs; i++) {
    Dart_Handle lib_url = Dart_ListGetAt(libs, i);
    EXPECT(Dart_IsString(lib_url));
    char const* chars;
    Dart_StringToCString(lib_url, &chars);
    printf("Lib %d: %s\n", i, chars);

    Dart_Handle scripts = Dart_GetScriptURLs(lib_url);
    EXPECT(Dart_IsList(scripts));
    intptr_t num_scripts;
    Dart_ListLength(scripts, &num_scripts);
    EXPECT(num_scripts >= 0);
    for (intptr_t i = 0; i < num_scripts; i++) {
      Dart_Handle script_url = Dart_ListGetAt(scripts, i);
      char const* chars;
      Dart_StringToCString(script_url, &chars);
      printf("  script %d: '%s'\n", i + 1, chars);
    }
  }

  Dart_Handle lib_url = Dart_NewString(TestCase::url());
  Dart_Handle source = Dart_GetScriptSource(lib_url, lib_url);
  EXPECT(Dart_IsString(source));
  char const* source_chars;
  Dart_StringToCString(source, &source_chars);
  printf("\n=== source: ===\n%s", source_chars);
  EXPECT_STREQ(kScriptChars, source_chars);
}

#endif  // defined(TARGET_ARCH_IA32) || defined(TARGET_ARCH_X64).

}  // namespace dart
