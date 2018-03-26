#include <SPIFlash.h>
#include <SPI.h>
#include <avr/wdt.h>

enum _consts
{
  flashChipSize = MB(4), fastRead = false, errorCheck = true
};

enum mode_t
{
  MODE_NONE, MODE_COMMAND, MODE_DATA
};

mode_t mode;

uint8_t data[KB(1)];

SPIFlash flash;

void
setup ()
{
  Serial.begin (115200);

  memset (data, 0, sizeof(data));
  mode = MODE_COMMAND;

#if defined (ARDUINO_SAMD_ZERO) || (__AVR_ATmega32U4__)
  while (!Serial)
  ; // Wait for Serial monitor to open
#endif

  if (flash.begin (flashChipSize))
    {
      Serial.println (F("Init OK!"));
    }
  else
    {
      Serial.println (F("Init FAIL!"));
    }
}

void
printResult (bool result)
{
  if (result)
    {
      Serial.println (F("OK"));
    }
  else
    {
      Serial.print (F("FAIL"));
      Serial.print (' ');
      flash.error (true);
    }
}

void
printResult (bool result, uint32_t addr)
{
  if (result)
    {
      Serial.print (F("OK"));
      Serial.print (F(" 0x"));
      Serial.print (addr, HEX);
      Serial.println ();
    }
  else
    {
      Serial.print (F("FAIL"));
      Serial.print (F(" 0x"));
      Serial.print (addr, HEX);
      Serial.print (' ');
      flash.error (true);
    }
}

void
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

  printResult (true);
}

void
eraseSector (uint32_t addr)
{
  bool result = flash.eraseSector (addr);
  printResult (result, addr);
}

void
readBuffer (uint32_t addr)
{
  bool result = flash.readByteArray (addr, data, sizeof(data), fastRead);

  Serial.write (data, sizeof(data));

  printResult (result, addr);
}

uint16_t
readData ()
{
  uint16_t idx = 0, len = sizeof(data), read;

  do
    {
      if ((idx % 32) == 0)
	{
	  Serial.print('+');
	}

      read = Serial.readBytes (data + idx, len);

      idx += read;
      len -= read;
    }
  while (len > 0);

  return idx;
}

void
readDataA ()
{
  for (uint16_t idx = 0; idx < sizeof(data); ++idx)
    {
      while (!Serial.available ())
        wdt_reset();
      data[idx] = Serial.read ();
    }
}

void
writeBuffer (uint32_t addr)
{
  readData ();

  bool result = flash.writeByteArray (addr, data, sizeof(data), errorCheck);

  printResult (result, addr);
}

void
loop ()
{
  wdt_reset();

  if (!Serial.available ())
    return;

  char cmd = Serial.read ();

  String addr = Serial.readString ();
  addr.trim ();

  switch (cmd)
    {
    case 'i':
      printInfo ();
      return;

    case 'a':
      printResult (flash.eraseChip ());
      return;

    case 'e':
      eraseSector (addr.toInt ());
      return;

    case 'w':
      writeBuffer (addr.toInt ());
      return;

    case 'W':
      printResult (true, readData ());
      return;

    case 'r':
      readBuffer (addr.toInt ());
      return;

    case 'F':
      for (uint32_t idx = 0; idx < sizeof(data); ++idx)
        {
          data[idx] = (uint8_t) (idx + 1);
        }
        /* no break */

    case 'R':
      Serial.write (data, sizeof(data));
      printResult (true);
      return;

    case '?':
      Serial.println (F("OK // SPI EEPROM FLASHER"));
      return;

    default:
      Serial.print (F("Unknown command: "));
      Serial.print (cmd);
      Serial.print (' ');
      Serial.print (addr);
      Serial.println ();

      Serial.println (F("OK // SPI EEPROM FLASHER"));
    }

}
