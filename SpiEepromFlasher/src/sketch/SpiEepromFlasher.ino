#include "Arduino.h"
#include <SPIFlash.h>
#include <SPI.h>
#include <avr/wdt.h>

enum _consts
{
  fastRead = false, errorCheck = true
};

uint8_t buf1k[KB(1)];

SPIFlash flash;

void
setup ()
{
  Serial.begin (115200);
#if defined (ARDUINO_SAMD_ZERO) || (__AVR_ATmega32U4__)
  while (!Serial)
  ; // Wait for Serial monitor to open
#endif

  if (flash.begin ())
    {
      Serial.println (F("Init OK!"));
    }
  else
    {
      Serial.println (F("Init FAIL!"));
    }
}

bool
printInfo ()
{
  uint64_t uid = flash.getUniqueID ();
  uint32_t JEDEC = flash.getJEDECID ();

  Serial.print (F("DeviceID: 0x"));
  Serial.print ((uint32_t) (uid >> 32), HEX);
  Serial.print ((uint32_t) (uid), HEX);
  Serial.println ();

  Serial.print (F("JEDEC ID: 0x"));
  Serial.print (JEDEC, HEX);
  Serial.println ();

  Serial.print (F("Manufacturer ID: 0x"));
  Serial.print ((uint8_t) (JEDEC >> 16), HEX);
  Serial.println ();

  Serial.print (F("Memory Type: 0x"));
  Serial.print ((uint8_t) (JEDEC >> 8), HEX);
  Serial.println ();

  Serial.print (F("Capacity: 0x"));
  Serial.print ((uint8_t) JEDEC, HEX);
  Serial.println ();

  Serial.print (F("Capacity: 0x"));
  Serial.print (flash.getCapacity (), HEX);
  Serial.println ();

  return true;
}

void
printResult (bool result)
{
  Serial.print (result ? F("OK") : F("FAIL"));
  Serial.println ();
}

void
printResult (bool result, uint32_t addr)
{
  Serial.print (result ? F("OK") : F("FAIL"));
  Serial.print (F(" 0x"));
  Serial.print (addr, HEX);
  Serial.println ();
}

uint32_t
getInt (const String& cmd)
{
  String s = cmd.substring (1);
  s.trim ();
  return s.toInt ();
}

void
loop ()
{
  if (!Serial.available ())
    return;

  String cmd = Serial.readString ();

  uint32_t addr;
  bool result;

  switch (cmd[0])
    {
    case 'i':
      result = printInfo ();
      printResult (result);
      return;

    case 'a':
      result = flash.eraseChip ();
      printResult (result);
      return;

    case 'e':
      addr = getInt (cmd);

      result = flash.eraseSector (addr);
      printResult (result, addr);
      return;

    case 'w':
      addr = getInt (cmd);

      for (uint16_t idx = 0; idx < sizeof(buf1k); ++idx)
        buf1k[idx] = Serial.read ();

      for (uint16_t offset = 0; offset < 1024; offset += 256)
        {
          addr += offset;
          result = flash.writeByteArray (addr, buf1k + offset, 256, errorCheck);
          if (!result)
            break;
        }

      printResult (result, result ? (addr - 1024) : addr);
      return;

    case 'r':
      addr = getInt (cmd);

      memset (buf1k, 0, sizeof(buf1k));

      for (uint16_t offset = 0; offset < 1024; offset += 256)
        {
          addr += offset;
          result = flash.readByteArray (addr, buf1k + offset, 256, fastRead);
          if (!result)
            break;
        }

      Serial.write (buf1k, sizeof(buf1k));

      Serial.println ();

      printResult (result, result ? (addr - 1024) : addr);
      return;

    case 'R':
      addr = getInt (cmd);

      memset (buf1k, 0, sizeof(buf1k));

      for (uint16_t offset = 0; offset < 1024; offset += 256)
        {
          addr += offset;
          result = flash.readByteArray (addr, buf1k + offset, 256, fastRead);
          if (!result)
            break;
        }

      for (uint16_t idx = 0; idx < sizeof(buf1k); ++idx)
        {
          if (buf1k[idx] < 0x10)
            Serial.print ('0');
          Serial.print (buf1k[idx], HEX);

          if ((idx & 0x1F) == 0x1F)
            Serial.println ();
          else
            {
              if ((idx & 0x0F) == 0x0F)
                Serial.print (' ');
              Serial.print (' ');
            }
        }

      Serial.println ();

      printResult (result, result ? (addr - 1024) : addr);
      return;

    case 'b':
      addr = getInt (cmd);

      Serial.print (F("[0x"));
      Serial.print (addr, HEX);
      Serial.print (F("] = 0x"));
      Serial.println (flash.readByte (addr, fastRead), HEX);
      Serial.print (F("OK"));
      Serial.println ();
      return;

    default:
      Serial.println (F("OK SPI EEPROM"));
      return;
    }

}
