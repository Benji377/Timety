// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'focus_models.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class SessionPhaseAdapter extends TypeAdapter<SessionPhase> {
  @override
  final int typeId = 23;

  @override
  SessionPhase read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return SessionPhase(
      type: fields[0] as PhaseType,
      durationMinutes: fields[1] as int,
    );
  }

  @override
  void write(BinaryWriter writer, SessionPhase obj) {
    writer
      ..writeByte(2)
      ..writeByte(0)
      ..write(obj.type)
      ..writeByte(1)
      ..write(obj.durationMinutes);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is SessionPhaseAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class FocusModeAdapter extends TypeAdapter<FocusMode> {
  @override
  final int typeId = 24;

  @override
  FocusMode read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return FocusMode(
      id: fields[0] as String,
      name: fields[1] as String,
      type: fields[2] as FocusModeType,
      phases: (fields[3] as List).cast<SessionPhase>(),
      isSystem: fields[4] as bool,
    );
  }

  @override
  void write(BinaryWriter writer, FocusMode obj) {
    writer
      ..writeByte(5)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.type)
      ..writeByte(3)
      ..write(obj.phases)
      ..writeByte(4)
      ..write(obj.isSystem);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is FocusModeAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class DistractionAdapter extends TypeAdapter<Distraction> {
  @override
  final int typeId = 25;

  @override
  Distraction read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Distraction(time: fields[0] as DateTime, note: fields[1] as String);
  }

  @override
  void write(BinaryWriter writer, Distraction obj) {
    writer
      ..writeByte(2)
      ..writeByte(0)
      ..write(obj.time)
      ..writeByte(1)
      ..write(obj.note);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is DistractionAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class FocusTagAdapter extends TypeAdapter<FocusTag> {
  @override
  final int typeId = 26;

  @override
  FocusTag read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return FocusTag(
      id: fields[0] as String,
      name: fields[1] as String,
      colorValue: fields[2] as int,
    );
  }

  @override
  void write(BinaryWriter writer, FocusTag obj) {
    writer
      ..writeByte(3)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.colorValue);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is FocusTagAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class FocusSessionAdapter extends TypeAdapter<FocusSession> {
  @override
  final int typeId = 20;

  @override
  FocusSession read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return FocusSession(
      id: fields[0] as String,
      modeId: fields[1] as String,
      startTime: fields[2] as DateTime,
      endTime: fields[3] as DateTime?,
      totalSecondsFocused: fields[4] as int,
      distractions: (fields[5] as List).cast<Distraction>(),
      isCompleted: fields[6] as bool,
      tagId: fields[7] as String?,
    );
  }

  @override
  void write(BinaryWriter writer, FocusSession obj) {
    writer
      ..writeByte(8)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.modeId)
      ..writeByte(2)
      ..write(obj.startTime)
      ..writeByte(3)
      ..write(obj.endTime)
      ..writeByte(4)
      ..write(obj.totalSecondsFocused)
      ..writeByte(5)
      ..write(obj.distractions)
      ..writeByte(6)
      ..write(obj.isCompleted)
      ..writeByte(7)
      ..write(obj.tagId);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is FocusSessionAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class FocusModeTypeAdapter extends TypeAdapter<FocusModeType> {
  @override
  final int typeId = 21;

  @override
  FocusModeType read(BinaryReader reader) {
    switch (reader.readByte()) {
      case 0:
        return FocusModeType.stopwatch;
      case 1:
        return FocusModeType.pomodoro;
      case 2:
        return FocusModeType.flexible;
      case 3:
        return FocusModeType.custom;
      default:
        return FocusModeType.stopwatch;
    }
  }

  @override
  void write(BinaryWriter writer, FocusModeType obj) {
    switch (obj) {
      case FocusModeType.stopwatch:
        writer.writeByte(0);
        break;
      case FocusModeType.pomodoro:
        writer.writeByte(1);
        break;
      case FocusModeType.flexible:
        writer.writeByte(2);
        break;
      case FocusModeType.custom:
        writer.writeByte(3);
        break;
    }
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is FocusModeTypeAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class PhaseTypeAdapter extends TypeAdapter<PhaseType> {
  @override
  final int typeId = 22;

  @override
  PhaseType read(BinaryReader reader) {
    switch (reader.readByte()) {
      case 0:
        return PhaseType.focus;
      case 1:
        return PhaseType.rest;
      default:
        return PhaseType.focus;
    }
  }

  @override
  void write(BinaryWriter writer, PhaseType obj) {
    switch (obj) {
      case PhaseType.focus:
        writer.writeByte(0);
        break;
      case PhaseType.rest:
        writer.writeByte(1);
        break;
    }
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PhaseTypeAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
