#include <TinyGPS++.h>
#include <SoftwareSerial.h>
#include <Wire.h>
#include <MPU6050.h>

static const int RXPin = 4, TXPin = 3;  //For the HC-05 Bluetooth module 
static const uint32_t AllBaud = 9600;   //General baudrate

int state;                              //Accidental inputs from the phone are stored. Can be ignored
int flag = 0;                           //flag is used in case an update needs to be sent after a pin is set HIGH for example
int integertest = 0;                    //General passthrough test
int ThresholdMPU = 0;                   //Threshold value of MPU, will be replaced with a crosscorrelation function
float latitude, longitude;              //storage for GPS location

float X[30];
const int n = sizeof(X)/sizeof(X[1]);   //define how many data points to save
const float treshold = 6;
int appendcounter;
bool holeDetected = false;

//All instances
TinyGPSPlus gps;
SoftwareSerial ss(RXPin, TXPin);
MPU6050 mpu;

void setup()
{
  Serial.begin(AllBaud);                //Open serial connection
  ss.begin(AllBaud);                    //Open bluetooth connection

  //MPU sensor check
  while(!mpu.begin(MPU6050_SCALE_2000DPS, MPU6050_RANGE_2G))
  {
    Serial.println("Could not find a valid MPU6050 sensor, check wiring!");
    delay(500);
  }
  
  pinMode(LED_BUILTIN, OUTPUT);         //Built-in LED on Arduino UNO for debugging purposes
}

void loop()
{
  Vector normAccel = mpu.readNormalizeAccel();            //get sensor values
  holeDetected = jerkMeter();                             //set bool
  if (holeDetected == true)
  {
    GPScheck(integertest, 1);
    for(int j = 0; j<n; j++)                              //destroy array, reset
    {
      X[j] = 0;
    }
  }
}

//This function is not used:
int SeverityReadoutMPU(int inttest)                       //Simply readout MPU data
{
  //Vector rawAccel = mpu.readRawAccel();                 //The specific MPU data is stored in a vector (X,Y and Z values)
  //Vector normGyro = mpu.readNormalizeGyro();
  Vector normAccel = mpu.readNormalizeAccel();
  int border = 0;                                         //Threshold which will be returned

  

  if (normAccel.ZAxis >= 12)                              //Threshold check
  {
    border = 1;                                           //verification that this is a hole (primitive)
    //Serial.print("AccZ = ");
    //Serial.println(normAccel.ZAxis);
    delay(10);
  }
  else
  {
    border = 0;
  }
  return border;                                          //border is returned, this is primitive thresholding
}

void GPScheck(int intpass, int severitypass)              //GPS check checks GPS availability based off the thresholding
{
  while ((ss.available() > 0) && severitypass > 0)        //Only check what is in the while loop when bluetooth is connected and threshold says there is a hole
  {
    if (gps.encode(ss.read()))                            //If there is gps encodeable from the bluetooth connection
    {
      digitalWrite(LED_BUILTIN, HIGH);                    //So both connections are online: let the built-in LED from the Arduino UNO blink
      delay(1000);
      digitalWrite(LED_BUILTIN, LOW);
      delay(1000);
      //noInterrupts();
      displayInfo(intpass, severitypass);                 //Pass the severity and integertest thru to displayInfo
      //interrupts();
      //delay(2000);
    }
  }

  if (millis() > 5000 && gps.charsProcessed() < 10)       //In case no connection with GPS can be made, make it noticeable
  {
    //Serial.println(F("No GPS detected: check wiring."));
    //while(true);                                        //Forces an indefinite loop of nothingness. Halts the entire process, so beware of uncommenting this.
  }
}

void displayInfo(int integertest, int severity)           //Print GPS location if valid, together with severity, else print 0
{
  Serial.println("finding location");
    if (gps.location.isValid() || millis() < 10000)       //find GPS within 10 seconds, otherwise just ignore this measurement
    {
      latitude = gps.location.lat();
      longitude = gps.location.lng();
      Serial.print(latitude, 10);
      Serial.print(F(":"));
      Serial.print(longitude, 10);
      Serial.print(F(":"));
      Serial.print(severity);
      holeDetected = false;
    }
    else
    {
      Serial.print("0.0000000000:0.0000000000:");
      Serial.print(severity);
    }  
    Serial.println();
    
}

void appendMeasurement(float measurement)
{  
  float  a = X[0];
  float temp;

  for (int i = 0; i<n-1;i++)              //put measured value in first item of array, shift others 1 cell
  {
     temp = X[i+1];
     X[i+1] = a;
     a = temp;
  }
  
  X[0] = measurement;
}


bool jerkMeter()                          //Use derivative of acc (jerk) to determine max jerk, check at jerk threshold
{
  float jerk[n];
  float maxvalue = 0;
  for(int i=0; i<(n-1); i++)
  {
    jerk[i] = X[i+1]-X[i];
    if(jerk[i]>maxvalue)
    {
      maxvalue = jerk[i];
    }      
  }
  if(maxvalue > treshold)
  {
    return true;
  }
  else
  {
    return false;
  }
}
