import 'dart:io';
import 'package:integration_test/integration_test_driver_extended.dart';

Future<void> main() async {
  await integrationDriver(
    onScreenshot:
        (
          String screenshotName,
          List<int> screenshotBytes, [
          Map<String, Object?>? args,
        ]) async {
          // Create the screenshots folder if it doesn't exist
          final directory = Directory('screenshots/output');
          if (!directory.existsSync()) {
            directory.createSync();
          }

          // Save the image to your computer
          final File image = File('${directory.path}/$screenshotName.png');
          image.writeAsBytesSync(screenshotBytes);

          return true;
        },
  );
}
