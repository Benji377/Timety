// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'habit_models.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class HabitAdapter extends TypeAdapter<Habit> {
  @override
  final int typeId = 30;

  @override
  Habit read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Habit(
      id: fields[0] as String,
      name: fields[1] as String,
      frequency: fields[2] as HabitFrequency,
      targetDaysPerWeek: fields[3] as int?,
      targetWeekdays: (fields[4] as List?)?.cast<int>(),
      targetTimeMinutes: fields[5] as int?,
      notes: fields[9] as String?,
      iconCodePoint: fields[10] as int?,
      stackName: fields[11] as String?,
      stackOrder: fields[12] as int?,
      completions: (fields[6] as List?)?.cast<DateTime>(),
      createdAt: fields[7] as DateTime?,
      colorValue: fields[8] as int?,
    );
  }

  @override
  void write(BinaryWriter writer, Habit obj) {
    writer
      ..writeByte(13)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.frequency)
      ..writeByte(3)
      ..write(obj.targetDaysPerWeek)
      ..writeByte(4)
      ..write(obj.targetWeekdays)
      ..writeByte(5)
      ..write(obj.targetTimeMinutes)
      ..writeByte(6)
      ..write(obj.completions)
      ..writeByte(7)
      ..write(obj.createdAt)
      ..writeByte(8)
      ..write(obj.colorValue)
      ..writeByte(9)
      ..write(obj.notes)
      ..writeByte(10)
      ..write(obj.iconCodePoint)
      ..writeByte(11)
      ..write(obj.stackName)
      ..writeByte(12)
      ..write(obj.stackOrder);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is HabitAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class HabitFrequencyAdapter extends TypeAdapter<HabitFrequency> {
  @override
  final int typeId = 31;

  @override
  HabitFrequency read(BinaryReader reader) {
    switch (reader.readByte()) {
      case 0:
        return HabitFrequency.daily;
      case 1:
        return HabitFrequency.weeklyExact;
      case 2:
        return HabitFrequency.weeklyFlexible;
      default:
        return HabitFrequency.daily;
    }
  }

  @override
  void write(BinaryWriter writer, HabitFrequency obj) {
    switch (obj) {
      case HabitFrequency.daily:
        writer.writeByte(0);
        break;
      case HabitFrequency.weeklyExact:
        writer.writeByte(1);
        break;
      case HabitFrequency.weeklyFlexible:
        writer.writeByte(2);
        break;
    }
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is HabitFrequencyAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
