package com.example.tt01remote.hid

/**
 * Raw USB/Bluetooth HID Report Descriptor describing two report types this
 * app can send: a standard keyboard report (for D-pad / select / back,
 * which Android's own input stack maps to DPAD/BACK system-wide) and a
 * Consumer Control report (for power / volume / mute / home, the usage
 * page real TV remotes use for those functions).
 *
 * Reference: USB HID Usage Tables (usb.org). Two Report IDs share one
 * descriptor so a single Bluetooth HID Device registration can send both
 * kinds of button presses.
 */
object HidDescriptor {

    const val REPORT_ID_KEYBOARD = 1
    const val REPORT_ID_CONSUMER = 2

    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ---- Keyboard collection (Report ID 1) ----
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x06,             // Usage (Keyboard)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(), //   Report ID (1)
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0.toByte(),    //   Usage Minimum (224)
        0x29, 0xE7.toByte(),    //   Usage Maximum (231)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95.toByte(), 0x08,    //   Report Count (8)
        0x81.toByte(), 0x02,    //   Input (Data,Var,Abs) - modifier byte
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x75, 0x08,             //   Report Size (8)
        0x81.toByte(), 0x01,    //   Input (Cnst) - reserved byte
        0x95.toByte(), 0x06,    //   Report Count (6)
        0x75, 0x08,             //   Report Size (8)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x65,             //   Logical Maximum (101)
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,             //   Usage Minimum (0)
        0x29, 0x65,             //   Usage Maximum (101)
        0x81.toByte(), 0x00,    //   Input (Data,Ary,Abs) - keycodes
        0xC0.toByte(),          // End Collection

        // ---- Consumer Control collection (Report ID 2) ----
        0x05, 0x0C,             // Usage Page (Consumer)
        0x09, 0x01,             // Usage (Consumer Control)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER.toByte(), //   Report ID (2)
        0x15, 0x00,             //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03, //   Logical Maximum (1023)
        0x19, 0x00,             //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03, //   Usage Maximum (1023)
        0x75, 0x10,             //   Report Size (16)
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x81.toByte(), 0x00,    //   Input (Data,Ary,Abs)
        0xC0.toByte()           // End Collection
    )

    // Keyboard/Keypad usage page (0x07) — Android maps these to DPAD/BACK/ENTER system-wide.
    object Key {
        const val UP = 0x52
        const val DOWN = 0x51
        const val LEFT = 0x50
        const val RIGHT = 0x4F
        const val SELECT = 0x28 // Enter
        const val BACK = 0x29   // Escape
    }

    // Consumer usage page (0x0C) — standard remote-control style functions.
    object Consumer {
        const val POWER = 0x0030
        const val HOME = 0x0223
        const val MENU = 0x0040
        const val VOLUME_UP = 0x00E9
        const val VOLUME_DOWN = 0x00EA
        const val MUTE = 0x00E2
    }
}
