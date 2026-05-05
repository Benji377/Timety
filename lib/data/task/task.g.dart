// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'task.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class TaskAdapter extends TypeAdapter<Task> {
  @override
  final int typeId = 0;

  @override
  Task read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Task(
      id: fields[0] as String,
      title: fields[1] as String,
      description: fields[2] as String,
      dueDate: fields[3] as DateTime?,
      location: fields[4] as String,
      priority: fields[5] as Priority,
      size: fields[8] as Size,
      reminders: (fields[6] as List).cast<DateTime>(),
      category: fields[7] as String,
      isCompleted: fields[9] as bool,
      completedAt: fields[10] as DateTime?,
      createdAt: fields[11] as DateTime,
    );
  }

  @override
  void write(BinaryWriter writer, Task obj) {
    writer
      ..writeByte(12)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.title)
      ..writeByte(2)
      ..write(obj.description)
      ..writeByte(3)
      ..write(obj.dueDate)
      ..writeByte(4)
      ..write(obj.location)
      ..writeByte(5)
      ..write(obj.priority)
      ..writeByte(6)
      ..write(obj.reminders)
      ..writeByte(7)
      ..write(obj.category)
      ..writeByte(8)
      ..write(obj.size)
      ..writeByte(9)
      ..write(obj.isCompleted)
      ..writeByte(10)
      ..write(obj.completedAt)
      ..writeByte(11)
      ..write(obj.createdAt);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is TaskAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class PriorityAdapter extends TypeAdapter<Priority> {
  @override
  final int typeId = 1;

  @override
  Priority read(BinaryReader reader) {
    switch (reader.readByte()) {
      case 0:
        return Priority.low;
      case 1:
        return Priority.medium;
      case 2:
        return Priority.high;
      case 3:
        return Priority.veryHigh;
      default:
        return Priority.low;
    }
  }

  @override
  void write(BinaryWriter writer, Priority obj) {
    switch (obj) {
      case Priority.low:
        writer.writeByte(0);
        break;
      case Priority.medium:
        writer.writeByte(1);
        break;
      case Priority.high:
        writer.writeByte(2);
        break;
      case Priority.veryHigh:
        writer.writeByte(3);
        break;
    }
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PriorityAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class SizeAdapter extends TypeAdapter<Size> {
  @override
  final int typeId = 2;

  @override
  Size read(BinaryReader reader) {
    switch (reader.readByte()) {
      case 0:
        return Size.small;
      case 1:
        return Size.medium;
      case 2:
        return Size.large;
      case 3:
        return Size.veryLarge;
      default:
        return Size.small;
    }
  }

  @override
  void write(BinaryWriter writer, Size obj) {
    switch (obj) {
      case Size.small:
        writer.writeByte(0);
        break;
      case Size.medium:
        writer.writeByte(1);
        break;
      case Size.large:
        writer.writeByte(2);
        break;
      case Size.veryLarge:
        writer.writeByte(3);
        break;
    }
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is SizeAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
