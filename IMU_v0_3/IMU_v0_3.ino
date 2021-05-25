#include <M5StickCPlus.h>

float accX = 0.0F;
float accY = 0.0F;
float accZ = 0.0F;

float gyroX = 0.0F;
float gyroY = 0.0F;
float gyroZ = 0.0F;

float pitch = 0.0F;
float roll  = 0.0F;
float yaw   = 0.0F;

float X = accX;
float Y = accY;
float Z = accZ;

float thresholdX = 0;
float thresholdY = 0;
float thresholdZ = 0;

float temp = 0;

void setup() {
  Serial.begin (9600);
  M5.begin();
  M5.IMU.Init();
  M5.Lcd.setRotation(3);
  M5.Lcd.fillScreen(BLUE);
  M5.Lcd.setTextSize(1);
  M5.Lcd.setCursor(40, 0);
  M5.Lcd.println("IMU TEST");
  M5.Lcd.setCursor(0, 10);
  M5.Lcd.println("  X       Y       Z");
  M5.Lcd.setCursor(0, 50);
  M5.Lcd.println("  Pitch   Roll    Yaw");
}


/*****************************************
M5.IMU.getGyroData(&gyroX,&gyroY,&gyroZ);
M5.IMU.getAccelData(&accX,&accY,&accZ);
M5.IMU.getAhrsData(&pitch,&roll,&yaw);
M5.IMU.getTempData(&temp);
*****************************************/

/*void printerfunction(accX, accY, accZ) {
  X = accX;
  Y = accY;
  Z = accZ;
  if (abs(accX) > 15) {
    Serial.println("X:     Y:     Z:");
    Serial.print("  %5.2f    %5.2f    %5.2f", X, Y, Z);
    Serial.println();
    delay(50);
  }
}
*/
void loop() {
  // put your main code here, to run repeatedly:
  M5.IMU.getGyroData(&gyroX,&gyroY,&gyroZ);
  M5.IMU.getAccelData(&accX,&accY,&accZ);
  M5.IMU.getAhrsData(&pitch,&roll,&yaw);
  M5.IMU.getTempData(&temp);
  
  M5.Lcd.setCursor(0, 20);
  M5.Lcd.printf("%6.2f  %6.2f  %6.2f      ", gyroX, gyroY, gyroZ);
  M5.Lcd.setCursor(140, 20);
  M5.Lcd.print("o/s");
  M5.Lcd.setCursor(0, 30);
  M5.Lcd.printf(" %5.2f   %5.2f   %5.2f   ", accX, accY, accZ);
  M5.Lcd.setCursor(140, 30);
  M5.Lcd.print("G");
  M5.Lcd.setCursor(0, 60);
  M5.Lcd.printf(" %5.2f   %5.2f   %5.2f   ", pitch, roll, yaw);

  M5.Lcd.setCursor(0, 70);
  M5.Lcd.printf("Temperature : %.2f C", temp);
  
  if (abs(accX) > thresholdX || abs(accY) > thresholdY || abs(accZ) > thresholdZ) {
    Serial.println("X:     Y:     Z:");
    Serial.print(gyroX);    //For some reason, the accelerometer data is stored in the gyro parameters....
    Serial.print("    ");
    Serial.print(gyroY);
    Serial.print("    ");
    Serial.print(gyroZ);
    Serial.println();
    //delay(50);
  }
  //if (abs(accX) > 10 || abs(accY) > 10 || abs(accZ) > 10) {
  //  M5.Lcd.fillScreen(BLACK);
  //  delay(1000);
  //}
  delay(100);

  //printerfunction(accX, accY, accZ);
  
}
