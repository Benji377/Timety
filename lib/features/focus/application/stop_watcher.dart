import 'package:timety/commons.dart';

class StopWatcher extends Notifier{
  DateTime? start;
  DateTime? end;
  Duration? totalTime;

  void timeStart(){
    start = DateTime.now();
    print(start);
  }

  void endFocusTime(){
    end = DateTime.now();
    if (start != null && end != null) {
      totalTime = end!.difference(start!);
    }
    print(end);
    print(totalTime);
  }
  
  @override
  build() {
    // TODO: implement build
    throw UnimplementedError();
  }

}