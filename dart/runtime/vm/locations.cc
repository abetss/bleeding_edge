// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/locations.h"

#include "vm/assembler.h"
#include "vm/il_printer.h"
#include "vm/intermediate_language.h"
#include "vm/flow_graph_compiler.h"
#include "vm/stack_frame.h"

namespace dart {

intptr_t RegisterSet::RegisterCount(intptr_t registers) {
  // Brian Kernighan's algorithm for counting the bits set.
  intptr_t count = 0;
  while (registers != 0) {
    ++count;
    registers &= (registers - 1);  // Clear the least significant bit set.
  }
  return count;
}


LocationSummary::LocationSummary(intptr_t input_count,
                                 intptr_t temp_count,
                                 LocationSummary::ContainsCall contains_call)
    : input_locations_(input_count),
      temp_locations_(temp_count),
      output_locations_(1),
      stack_bitmap_(NULL),
      contains_call_(contains_call),
      live_registers_() {
  for (intptr_t i = 0; i < input_count; i++) {
    input_locations_.Add(Location());
  }
  for (intptr_t i = 0; i < temp_count; i++) {
    temp_locations_.Add(Location());
  }
  output_locations_.Add(Location());
  ASSERT(output_locations_.length() == 1);
  if (contains_call_ != kNoCall) {
    stack_bitmap_ = new BitmapBuilder();
  }
}


LocationSummary::LocationSummary(intptr_t input_count,
                                intptr_t temp_count,
                                intptr_t output_count,
                                LocationSummary::ContainsCall contains_call)
    : input_locations_(input_count),
      temp_locations_(temp_count),
      output_locations_(output_count),
      stack_bitmap_(NULL),
      contains_call_(contains_call),
      live_registers_() {
  for (intptr_t i = 0; i < input_count; i++) {
    input_locations_.Add(Location());
  }
  for (intptr_t i = 0; i < temp_count; i++) {
    temp_locations_.Add(Location());
  }
  // TODO(johnmccutchan): Remove this assertion once support for multiple
  // outputs is complete.
  ASSERT(output_count == 1);
  for (intptr_t i = 0; i < output_count; i++) {
    output_locations_.Add(Location());
  }
  if (contains_call_ != kNoCall) {
    stack_bitmap_ = new BitmapBuilder();
  }
}


LocationSummary* LocationSummary::Make(
    intptr_t input_count,
    Location out,
    LocationSummary::ContainsCall contains_call) {
  LocationSummary* summary = new LocationSummary(input_count, 0, contains_call);
  for (intptr_t i = 0; i < input_count; i++) {
    summary->set_in(i, Location::RequiresRegister());
  }
  summary->set_out(0, out);
  return summary;
}


Location Location::Pair(Location first, Location second) {
  PairLocation* pair_location = new PairLocation();
  ASSERT((reinterpret_cast<intptr_t>(pair_location) & kLocationTagMask) == 0);
  pair_location->SetAt(0, first);
  pair_location->SetAt(1, second);
  Location loc(reinterpret_cast<uword>(pair_location) | kPairLocationTag);
  return loc;
}


PairLocation* Location::AsPairLocation() const {
  ASSERT(IsPairLocation());
  return reinterpret_cast<PairLocation*>(value_ & ~kLocationTagMask);
}


Location Location::RegisterOrConstant(Value* value) {
  ConstantInstr* constant = value->definition()->AsConstant();
  return ((constant != NULL) && Assembler::IsSafe(constant->value()))
      ? Location::Constant(constant->value())
      : Location::RequiresRegister();
}


Location Location::RegisterOrSmiConstant(Value* value) {
  ConstantInstr* constant = value->definition()->AsConstant();
  return ((constant != NULL) && Assembler::IsSafeSmi(constant->value()))
      ? Location::Constant(constant->value())
      : Location::RequiresRegister();
}


Location Location::FixedRegisterOrConstant(Value* value, Register reg) {
  ConstantInstr* constant = value->definition()->AsConstant();
  return ((constant != NULL) && Assembler::IsSafe(constant->value()))
      ? Location::Constant(constant->value())
      : Location::RegisterLocation(reg);
}


Location Location::FixedRegisterOrSmiConstant(Value* value, Register reg) {
  ConstantInstr* constant = value->definition()->AsConstant();
  return ((constant != NULL) && Assembler::IsSafeSmi(constant->value()))
      ? Location::Constant(constant->value())
      : Location::RegisterLocation(reg);
}


Location Location::AnyOrConstant(Value* value) {
  ConstantInstr* constant = value->definition()->AsConstant();
  return ((constant != NULL) && Assembler::IsSafe(constant->value()))
      ? Location::Constant(constant->value())
      : Location::Any();
}


Address Location::ToStackSlotAddress() const {
  const intptr_t index = stack_index();
  if (index < 0) {
    const intptr_t offset = (kParamEndSlotFromFp - index)  * kWordSize;
    return Address(FPREG, offset);
  } else {
    const intptr_t offset = (kFirstLocalSlotFromFp - index) * kWordSize;
    return Address(FPREG, offset);
  }
}


intptr_t Location::ToStackSlotOffset() const {
  const intptr_t index = stack_index();
  if (index < 0) {
    const intptr_t offset = (kParamEndSlotFromFp - index)  * kWordSize;
    return offset;
  } else {
    const intptr_t offset = (kFirstLocalSlotFromFp - index) * kWordSize;
    return offset;
  }
}


const char* Location::Name() const {
  switch (kind()) {
    case kInvalid: return "?";
    case kRegister: return Assembler::RegisterName(reg());
    case kFpuRegister: return Assembler::FpuRegisterName(fpu_reg());
    case kStackSlot: return "S";
    case kDoubleStackSlot: return "DS";
    case kQuadStackSlot: return "QS";
    case kUnallocated:
      switch (policy()) {
        case kAny:
          return "A";
        case kPrefersRegister:
          return "P";
        case kRequiresRegister:
          return "R";
        case kRequiresFpuRegister:
          return "DR";
        case kWritableRegister:
          return "WR";
        case kSameAsFirstInput:
          return "0";
      }
      UNREACHABLE();
    default:
      if (IsConstant()) {
        return "C";
      } else {
        ASSERT(IsPairLocation());
        return "2P";
      }
  }
  return "?";
}


void Location::PrintTo(BufferFormatter* f) const {
  if (kind() == kStackSlot) {
    f->Print("S%+" Pd "", stack_index());
  } else if (kind() == kDoubleStackSlot) {
    f->Print("DS%+" Pd "", stack_index());
  } else if (kind() == kQuadStackSlot) {
    f->Print("QS%+" Pd "", stack_index());
  } else {
    f->Print("%s", Name());
  }
}


const char* Location::ToCString() const {
  char buffer[1024];
  BufferFormatter bf(buffer, 1024);
  PrintTo(&bf);
  return Isolate::Current()->current_zone()->MakeCopyOfString(buffer);
}


void Location::Print() const {
  if (kind() == kStackSlot) {
    OS::Print("S%+" Pd "", stack_index());
  } else {
    OS::Print("%s", Name());
  }
}


void LocationSummary::PrintTo(BufferFormatter* f) const {
  if (input_count() > 0) {
    f->Print(" (");
    for (intptr_t i = 0; i < input_count(); i++) {
      if (i != 0) f->Print(", ");
      in(i).PrintTo(f);
    }
    f->Print(")");
  }

  if (temp_count() > 0) {
    f->Print(" [");
    for (intptr_t i = 0; i < temp_count(); i++) {
      if (i != 0) f->Print(", ");
      temp(i).PrintTo(f);
    }
    f->Print("]");
  }

  if (!out(0).IsInvalid()) {
    f->Print(" => ");
    out(0).PrintTo(f);
  }

  if (always_calls()) f->Print(" C");
}

}  // namespace dart
