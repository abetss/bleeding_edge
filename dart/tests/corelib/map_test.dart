// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library map_test;
import "package:expect/expect.dart";
import 'dart:collection';

void main() {
  test(new HashMap());
  test(new LinkedHashMap());
  test(new SplayTreeMap());
  test(new SplayTreeMap(Comparable.compare));
  testLinkedHashMap();
  testMapLiteral();
  testNullValue();
  testTypes();

  testWeirdStringKeys(new Map());
  testWeirdStringKeys(new Map<String, String>());
  testWeirdStringKeys(new HashMap());
  testWeirdStringKeys(new HashMap<String, String>());
  testWeirdStringKeys(new LinkedHashMap());
  testWeirdStringKeys(new LinkedHashMap<String, String>());
  testWeirdStringKeys(new SplayTreeMap());
  testWeirdStringKeys(new SplayTreeMap<String, String>());

  testNumericKeys(new Map());
  testNumericKeys(new Map<num, String>());
  testNumericKeys(new HashMap());
  testNumericKeys(new HashMap<num, String>());
  testNumericKeys(new HashMap(equals: identical));
  testNumericKeys(new HashMap<num, String>(equals: identical));
  testNumericKeys(new LinkedHashMap());
  testNumericKeys(new LinkedHashMap<num, String>());
  testNumericKeys(new LinkedHashMap(equals: identical));
  testNumericKeys(new LinkedHashMap<num, String>(equals: identical));

  testNaNKeys(new Map());
  testNaNKeys(new Map<num, String>());
  testNaNKeys(new HashMap());
  testNaNKeys(new HashMap<num, String>());
  testNaNKeys(new LinkedHashMap());
  testNaNKeys(new LinkedHashMap<num, String>());
  // Identity maps fail the NaN-keys tests because the test assumes that
  // NaN is not equal to NaN.

  testIdentityMap(new HashMap(equals: identical));
  testIdentityMap(new LinkedHashMap(equals: identical));

  testCustomMap(new HashMap(equals: myEquals, hashCode: myHashCode,
                            isValidKey: (v) => v is Customer));
  testCustomMap(new LinkedHashMap(equals: myEquals, hashCode: myHashCode,
                                  isValidKey: (v) => v is Customer));
  testCustomMap(new HashMap<Customer,dynamic>(equals: myEquals,
                                              hashCode: myHashCode));

  testCustomMap(new LinkedHashMap<Customer,dynamic>(equals: myEquals,
                                                    hashCode: myHashCode));

  testIterationOrder(new LinkedHashMap());
  testIterationOrder(new LinkedHashMap(equals: identical));

  testOtherKeys(new SplayTreeMap<int, int>());
  testOtherKeys(new SplayTreeMap<int, int>((int a, int b) => a - b,
                                           (v) => v is int));
  testOtherKeys(new SplayTreeMap((int a, int b) => a - b,
                                 (v) => v is int));
  testOtherKeys(new HashMap<int, int>());
  testOtherKeys(new HashMap<int, int>(equals: identical));
  testOtherKeys(new HashMap<int, int>(hashCode: (v) => v.hashCode,
                                      isValidKey: (v) => v is int));
  testOtherKeys(new HashMap(equals: (int x, int y) => x == y,
                            hashCode: (int v) => v.hashCode,
                            isValidKey: (v) => v is int));
  testOtherKeys(new LinkedHashMap<int, int>());
  testOtherKeys(new LinkedHashMap<int, int>(equals: identical));
  testOtherKeys(new LinkedHashMap<int, int>(hashCode: (v) => v.hashCode,
                                       isValidKey: (v) => v is int));
  testOtherKeys(new LinkedHashMap(equals: (int x, int y) => x == y,
                                  hashCode: (int v) => v.hashCode,
                                  isValidKey: (v) => v is int));
}


void test(Map map) {
  testDeletedElement(map);
  testMap(map, 1, 2, 3, 4, 5, 6, 7, 8);
  map.clear();
  testMap(map, "value1", "value2", "value3", "value4", "value5",
          "value6", "value7", "value8");
}

void testLinkedHashMap() {
  LinkedHashMap map = new LinkedHashMap();
  Expect.equals(false, map.containsKey(1));
  map[1] = 1;
  map[1] = 2;
  testLength(1, map);
}

void testMap(Map map, key1, key2, key3, key4, key5, key6, key7, key8) {
  int value1 = 10;
  int value2 = 20;
  int value3 = 30;
  int value4 = 40;
  int value5 = 50;
  int value6 = 60;
  int value7 = 70;
  int value8 = 80;

  testLength(0, map);

  map[key1] = value1;
  Expect.equals(value1, map[key1]);
  map[key1] = value2;
  Expect.equals(false, map.containsKey(key2));
  testLength(1, map);

  map[key1] = value1;
  Expect.equals(value1, map[key1]);
  // Add enough entries to make sure the table grows.
  map[key2] = value2;
  Expect.equals(value2, map[key2]);
  testLength(2, map);
  map[key3] = value3;
  Expect.equals(value2, map[key2]);
  Expect.equals(value3, map[key3]);
  map[key4] = value4;
  Expect.equals(value3, map[key3]);
  Expect.equals(value4, map[key4]);
  map[key5] = value5;
  Expect.equals(value4, map[key4]);
  Expect.equals(value5, map[key5]);
  map[key6] = value6;
  Expect.equals(value5, map[key5]);
  Expect.equals(value6, map[key6]);
  map[key7] = value7;
  Expect.equals(value6, map[key6]);
  Expect.equals(value7, map[key7]);
  map[key8] = value8;
  Expect.equals(value1, map[key1]);
  Expect.equals(value2, map[key2]);
  Expect.equals(value3, map[key3]);
  Expect.equals(value4, map[key4]);
  Expect.equals(value5, map[key5]);
  Expect.equals(value6, map[key6]);
  Expect.equals(value7, map[key7]);
  Expect.equals(value8, map[key8]);
  testLength(8, map);

  map.remove(key4);
  Expect.equals(false, map.containsKey(key4));
  testLength(7, map);

  // Test clearing the table.
  map.clear();
  testLength(0, map);
  Expect.equals(false, map.containsKey(key1));
  Expect.equals(false, map.containsKey(key2));
  Expect.equals(false, map.containsKey(key3));
  Expect.equals(false, map.containsKey(key4));
  Expect.equals(false, map.containsKey(key5));
  Expect.equals(false, map.containsKey(key6));
  Expect.equals(false, map.containsKey(key7));
  Expect.equals(false, map.containsKey(key8));

  // Test adding and removing again.
  map[key1] = value1;
  Expect.equals(value1, map[key1]);
  testLength(1, map);
  map[key2] = value2;
  Expect.equals(value2, map[key2]);
  testLength(2, map);
  map[key3] = value3;
  Expect.equals(value3, map[key3]);
  map.remove(key3);
  testLength(2, map);
  map[key4] = value4;
  Expect.equals(value4, map[key4]);
  map.remove(key4);
  testLength(2, map);
  map[key5] = value5;
  Expect.equals(value5, map[key5]);
  map.remove(key5);
  testLength(2, map);
  map[key6] = value6;
  Expect.equals(value6, map[key6]);
  map.remove(key6);
  testLength(2, map);
  map[key7] = value7;
  Expect.equals(value7, map[key7]);
  map.remove(key7);
  testLength(2, map);
  map[key8] = value8;
  Expect.equals(value8, map[key8]);
  map.remove(key8);
  testLength(2, map);

  Expect.equals(true, map.containsKey(key1));
  Expect.equals(true, map.containsValue(value1));

  // Test Map.forEach.
  Map otherMap = new Map();
  void testForEachMap(key, value) {
    otherMap[key] = value;
  }
  map.forEach(testForEachMap);
  Expect.equals(true, otherMap.containsKey(key1));
  Expect.equals(true, otherMap.containsKey(key2));
  Expect.equals(true, otherMap.containsValue(value1));
  Expect.equals(true, otherMap.containsValue(value2));
  Expect.equals(2, otherMap.length);

  otherMap.clear();
  Expect.equals(0, otherMap.length);

  // Test Collection.keys.
  void testForEachCollection(value) {
    otherMap[value] = value;
  }
  Iterable keys = map.keys;
  keys.forEach(testForEachCollection);
  Expect.equals(true, otherMap.containsKey(key1));
  Expect.equals(true, otherMap.containsKey(key2));
  Expect.equals(true, otherMap.containsValue(key1));
  Expect.equals(true, otherMap.containsValue(key2));
  Expect.equals(true, !otherMap.containsKey(value1));
  Expect.equals(true, !otherMap.containsKey(value2));
  Expect.equals(true, !otherMap.containsValue(value1));
  Expect.equals(true, !otherMap.containsValue(value2));
  Expect.equals(2, otherMap.length);
  otherMap.clear();
  Expect.equals(0, otherMap.length);

  // Test Collection.values.
  Iterable values = map.values;
  values.forEach(testForEachCollection);
  Expect.equals(true, !otherMap.containsKey(key1));
  Expect.equals(true, !otherMap.containsKey(key2));
  Expect.equals(true, !otherMap.containsValue(key1));
  Expect.equals(true, !otherMap.containsValue(key2));
  Expect.equals(true, otherMap.containsKey(value1));
  Expect.equals(true, otherMap.containsKey(value2));
  Expect.equals(true, otherMap.containsValue(value1));
  Expect.equals(true, otherMap.containsValue(value2));
  Expect.equals(2, otherMap.length);
  otherMap.clear();
  Expect.equals(0, otherMap.length);

  // Test Map.putIfAbsent.
  map.clear();
  Expect.equals(false, map.containsKey(key1));
  map.putIfAbsent(key1, () => 10);
  Expect.equals(true, map.containsKey(key1));
  Expect.equals(10, map[key1]);
  Expect.equals(10,
      map.putIfAbsent(key1, () => 11));

  // Test Map.addAll.
  map.clear();
  otherMap.clear();
  otherMap[99] = 1;
  otherMap[50] = 50;
  otherMap[1] = 99;
  map.addAll(otherMap);
  Expect.equals(3, map.length);
  Expect.equals(1, map[99]);
  Expect.equals(50, map[50]);
  Expect.equals(99, map[1]);
  otherMap[50] = 42;
  map.addAll(new HashMap.from(otherMap));
  Expect.equals(3, map.length);
  Expect.equals(1, map[99]);
  Expect.equals(42, map[50]);
  Expect.equals(99, map[1]);
  otherMap[99] = 7;
  map.addAll(new SplayTreeMap.from(otherMap));
  Expect.equals(3, map.length);
  Expect.equals(7, map[99]);
  Expect.equals(42, map[50]);
  Expect.equals(99, map[1]);
  otherMap.remove(99);
  map[99] = 0;
  map.addAll(otherMap);
  Expect.equals(3, map.length);
  Expect.equals(0, map[99]);
  Expect.equals(42, map[50]);
  Expect.equals(99, map[1]);
  map.clear();
  otherMap.clear();
  map.addAll(otherMap);
  Expect.equals(0, map.length);
}

void testDeletedElement(Map map) {
  map.clear();
  for (int i = 0; i < 100; i++) {
    map[1] = 2;
    testLength(1, map);
    map.remove(1);
    testLength(0, map);
  }
  testLength(0, map);
}

void testMapLiteral() {
  Map m = {"a": 1, "b" : 2, "c": 3 };
  Expect.equals(3, m.length);
  int sum = 0;
  m.forEach((a, b) {
    sum += b;
  });
  Expect.equals(6, sum);

  List values = m.keys.toList();
  Expect.equals(3, values.length);
  String first = values[0];
  String second = values[1];
  String third = values[2];
  String all = "${first}${second}${third}";
  Expect.equals(3, all.length);
  Expect.equals(true, all.contains("a", 0));
  Expect.equals(true, all.contains("b", 0));
  Expect.equals(true, all.contains("c", 0));
}

void testNullValue() {
  Map m = {"a": 1, "b" : null, "c": 3 };

  Expect.equals(null, m["b"]);
  Expect.equals(true, m.containsKey("b"));
  Expect.equals(3, m.length);

  m["a"] = null;
  m["c"] = null;
  Expect.equals(null, m["a"]);
  Expect.equals(true, m.containsKey("a"));
  Expect.equals(null, m["c"]);
  Expect.equals(true, m.containsKey("c"));
  Expect.equals(3, m.length);

  m.remove("a");
  Expect.equals(2, m.length);
  Expect.equals(null, m["a"]);
  Expect.equals(false, m.containsKey("a"));
}

void testTypes() {
  Map<int, dynamic> map;
  testMap(Map map) {
    map[42] = "text";
    map[43] = "text";
    map[42] = "text";
    map.remove(42);
    map[42] = "text";
  }
  testMap(new HashMap<int, String>());
  testMap(new LinkedHashMap<int, String>());
  testMap(new SplayTreeMap<int, String>());
  testMap(new SplayTreeMap<int, String>(Comparable.compare));
  testMap(new SplayTreeMap<int, String>((int a, int b) => a.compareTo(b)));
  testMap(new HashMap<num, String>());
  testMap(new LinkedHashMap<num, String>());
  testMap(new SplayTreeMap<num, String>());
  testMap(new SplayTreeMap<num, String>(Comparable.compare));
  testMap(new SplayTreeMap<num, String>((num a, num b) => a.compareTo(b)));
}

void testWeirdStringKeys(Map map) {
  // Test weird keys.
  var weirdKeys = const [
      'hasOwnProperty',
      'constructor',
      'toLocaleString',
      'propertyIsEnumerable',
      '__defineGetter__',
      '__defineSetter__',
      '__lookupGetter__',
      '__lookupSetter__',
      'isPrototypeOf',
      'toString',
      'valueOf',
      '__proto__',
      '__count__',
      '__parent__',
      ''];
  Expect.isTrue(map.isEmpty);
  for (var key in weirdKeys) {
    Expect.isFalse(map.containsKey(key));
    Expect.equals(null, map[key]);
    var value = 'value:$key';
    map[key] = value;
    Expect.isTrue(map.containsKey(key));
    Expect.equals(value, map[key]);
    Expect.equals(value, map.remove(key));
    Expect.isFalse(map.containsKey(key));
    Expect.equals(null, map[key]);
  }
  Expect.isTrue(map.isEmpty);

}

void testNumericKeys(Map map) {
  var numericKeys = const [
      double.INFINITY,
      double.NEGATIVE_INFINITY,
      0,
      0.0,
      -0.0 ];

  Expect.isTrue(map.isEmpty);
  for (var key in numericKeys) {
    Expect.isFalse(map.containsKey(key));
    Expect.equals(null, map[key]);
    var value = 'value:$key';
    map[key] = value;
    Expect.isTrue(map.containsKey(key));
    Expect.equals(value, map[key]);
    Expect.equals(value, map.remove(key));
    Expect.isFalse(map.containsKey(key));
    Expect.equals(null, map[key]);
  }
  Expect.isTrue(map.isEmpty);
}

void testNaNKeys(Map map) {
  Expect.isTrue(map.isEmpty);
  // Test NaN.
  var nan = double.NAN;
  Expect.isFalse(map.containsKey(nan));
  Expect.equals(null, map[nan]);

  map[nan] = 'value:0';
  Expect.isFalse(map.containsKey(nan));
  Expect.equals(null, map[nan]);
  testLength(1, map);

  map[nan] = 'value:1';
  Expect.isFalse(map.containsKey(nan));
  Expect.equals(null, map[nan]);
  testLength(2, map);

  Expect.equals(null, map.remove(nan));
  testLength(2, map);

  var count = 0;
  map.forEach((key, value) {
    if (key.isNaN) count++;
  });
  Expect.equals(2, count);

  map.clear();
  Expect.isTrue(map.isEmpty);
}

void testLength(int length, Map map) {
  Expect.equals(length, map.length);
  (length == 0 ? Expect.isTrue : Expect.isFalse)(map.isEmpty);
  (length != 0 ? Expect.isTrue : Expect.isFalse)(map.isNotEmpty);
}


testIdentityMap(Map map) {
  Expect.isTrue(map.isEmpty);

  var nan = double.NAN;
  // TODO(11551): Remove guard when dart2js makes identical(NaN, NaN) true.
  if (identical(nan, nan)) {
    map[nan] = 42;
    testLength(1, map);
    Expect.isTrue(map.containsKey(nan));
    Expect.equals(42, map[nan]);
    map[nan] = 37;
    testLength(1, map);
    Expect.equals(37, map[nan]);
    Expect.equals(37, map.remove(nan));
    testLength(0, map);
  }

  Vampire v1 = const Vampire(1);
  Vampire v2 = const Vampire(2);
  Expect.isFalse(v1 == v1);
  Expect.isFalse(v2 == v2);
  Expect.isTrue(v2 == v1);  // Snob!

  map[v1] = 1;
  map[v2] = 2;
  testLength(2, map);

  Expect.isTrue(map.containsKey(v1));
  Expect.isTrue(map.containsKey(v2));

  Expect.equals(1, map[v1]);
  Expect.equals(2, map[v2]);

  Expect.equals(1, map.remove(v1));
  testLength(1, map);
  Expect.isFalse(map.containsKey(v1));
  Expect.isTrue(map.containsKey(v2));

  Expect.isNull(map.remove(v1));
  Expect.equals(2, map.remove(v2));
  testLength(0, map);

  var eq01 = new Equalizer(0);
  var eq02 = new Equalizer(0);
  var eq11 = new Equalizer(1);
  var eq12 = new Equalizer(1);
  // Sanity.
  Expect.equals(eq01, eq02);
  Expect.equals(eq02, eq01);
  Expect.equals(eq11, eq12);
  Expect.equals(eq12, eq11);
  Expect.notEquals(eq01, eq11);
  Expect.notEquals(eq01, eq12);
  Expect.notEquals(eq02, eq11);
  Expect.notEquals(eq02, eq12);
  Expect.notEquals(eq11, eq01);
  Expect.notEquals(eq11, eq02);
  Expect.notEquals(eq12, eq01);
  Expect.notEquals(eq12, eq02);

  map[eq01] = 0;
  map[eq02] = 1;
  map[eq11] = 2;
  map[eq12] = 3;
  testLength(4, map);

  Expect.equals(0, map[eq01]);
  Expect.equals(1, map[eq02]);
  Expect.equals(2, map[eq11]);
  Expect.equals(3, map[eq12]);

  Expect.isTrue(map.containsKey(eq01));
  Expect.isTrue(map.containsKey(eq02));
  Expect.isTrue(map.containsKey(eq11));
  Expect.isTrue(map.containsKey(eq12));

  Expect.equals(1, map.remove(eq02));
  Expect.equals(3, map.remove(eq12));
  testLength(2, map);
  Expect.isTrue(map.containsKey(eq01));
  Expect.isFalse(map.containsKey(eq02));
  Expect.isTrue(map.containsKey(eq11));
  Expect.isFalse(map.containsKey(eq12));

  Expect.equals(0, map[eq01]);
  Expect.equals(null, map[eq02]);
  Expect.equals(2, map[eq11]);
  Expect.equals(null, map[eq12]);

  Expect.equals(0, map.remove(eq01));
  Expect.equals(2, map.remove(eq11));
  testLength(0, map);

  map[eq01] = 0;
  map[eq02] = 1;
  map[eq11] = 2;
  map[eq12] = 3;
  testLength(4, map);

  // Transfer to equality-based map will collapse elements.
  Map eqMap = new HashMap();
  eqMap.addAll(map);
  testLength(2, eqMap);
  Expect.isTrue(eqMap.containsKey(eq01));
  Expect.isTrue(eqMap.containsKey(eq02));
  Expect.isTrue(eqMap.containsKey(eq11));
  Expect.isTrue(eqMap.containsKey(eq12));
}

/** Class of objects that are equal if they hold the same id. */
class Equalizer {
  int id;
  Equalizer(this.id);
  int get hashCode => id;
  bool operator==(Object other) =>
      other is Equalizer && id == (other as Equalizer).id;
}

/**
 * Objects that are not reflexive.
 *
 * They think they are better than their equals.
 */
class Vampire {
  final int generation;
  const Vampire(this.generation);

  int get hashCode => generation;

  // The double-fang operator falsely claims that a vampire is equal to
  // any of its sire's generation.
  bool operator==(Object other) =>
      other is Vampire && generation - 1 == (other as Vampire).generation;
}

void testCustomMap(Map map) {
  testLength(0, map);
  var c11 = const Customer(1, 1);
  var c12 = const Customer(1, 2);
  var c21 = const Customer(2, 1);
  var c22 = const Customer(2, 2);
  // Sanity.
  Expect.equals(c11, c12);
  Expect.notEquals(c11, c21);
  Expect.notEquals(c11, c22);
  Expect.equals(c21, c22);
  Expect.notEquals(c21, c11);
  Expect.notEquals(c21, c12);

  Expect.isTrue(myEquals(c11, c21));
  Expect.isFalse(myEquals(c11, c12));
  Expect.isFalse(myEquals(c11, c22));
  Expect.isTrue(myEquals(c12, c22));
  Expect.isFalse(myEquals(c12, c11));
  Expect.isFalse(myEquals(c12, c21));

  map[c11] = 42;
  testLength(1, map);
  Expect.isTrue(map.containsKey(c11));
  Expect.isTrue(map.containsKey(c21));
  Expect.isFalse(map.containsKey(c12));
  Expect.isFalse(map.containsKey(c22));
  Expect.equals(42, map[c11]);
  Expect.equals(42, map[c21]);

  map[c21] = 37;
  testLength(1, map);
  Expect.isTrue(map.containsKey(c11));
  Expect.isTrue(map.containsKey(c21));
  Expect.isFalse(map.containsKey(c12));
  Expect.isFalse(map.containsKey(c22));
  Expect.equals(37, map[c11]);
  Expect.equals(37, map[c21]);

  map[c22] = 42;
  testLength(2, map);
  Expect.isTrue(map.containsKey(c11));
  Expect.isTrue(map.containsKey(c21));
  Expect.isTrue(map.containsKey(c12));
  Expect.isTrue(map.containsKey(c22));
  Expect.equals(37, map[c11]);
  Expect.equals(37, map[c21]);
  Expect.equals(42, map[c12]);
  Expect.equals(42, map[c22]);

  Expect.equals(42, map.remove(c12));
  testLength(1, map);
  Expect.isTrue(map.containsKey(c11));
  Expect.isTrue(map.containsKey(c21));
  Expect.isFalse(map.containsKey(c12));
  Expect.isFalse(map.containsKey(c22));
  Expect.equals(37, map[c11]);
  Expect.equals(37, map[c21]);

  Expect.equals(37, map.remove(c11));
  testLength(0, map);
}

class Customer {
  final int id;
  final int secondId;
  const Customer(this.id, this.secondId);
  int get hashCode => id;
  bool operator==(Object other) {
    if (other is! Customer) return false;
    Customer otherCustomer = other;
    return id == otherCustomer.id;
  }
}

int myHashCode(Customer c) => c.secondId;
bool myEquals(Customer a, Customer b) => a.secondId == b.secondId;

void testIterationOrder(Map map) {
  var order = [0, 6, 4, 2, 7, 9, 7, 1, 2, 5, 3];
  for (int i = 0; i < order.length; i++) map[order[i]] = i;
  Expect.listEquals(map.keys.toList(), [0, 6, 4, 2, 7, 9, 1, 5, 3]);
  Expect.listEquals(map.values.toList(), [0, 1, 2, 8, 6, 5, 7, 9, 10]);
}

void testOtherKeys(Map<int, int> map) {
  // Test that non-int keys are allowed in containsKey/remove/lookup.
  // Custom hash sets and tree sets must be constructed so they don't
  // use the equality/comparator on incompatible objects.

  // This should not throw in either checked or unchecked mode.
  map[0] = 0;
  map[1] = 1;
  map[2] = 2;
  Expect.isFalse(map.containsKey("not an int"));
  Expect.isFalse(map.containsKey(1.5));
  Expect.isNull(map.remove("not an int"));
  Expect.isNull(map.remove(1.5));
  Expect.isNull(map["not an int"]);
  Expect.isNull(map[1.5]);
}
