package dev.falaris.client.gui.click;

public final class UiColor {
    private UiColor() {}

    public static int rgb(int r, int g, int b) { return argb(255, r, g, b); }
    public static int argb(int a, int r, int g, int b) { return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF); }
    public static int withAlpha(int c, int a) { return (a & 0xFF) << 24 | (c & 0xFFFFFF); }

    // Surface colors
    public static int surface()       { return rgb(18, 20, 30); }
    public static int surface2()      { return rgb(22, 24, 36); }
    public static int surface3()      { return rgb(28, 30, 44); }
    public static int surface4()      { return rgb(34, 36, 52); }
    public static int hover1()        { return rgb(40, 42, 60); }
    public static int hover2()        { return rgb(46, 48, 68); }

    // Purple accent palette
    public static int accentDark()    { return rgb(90, 60, 160); }
    public static int accent()        { return rgb(130, 100, 210); }
    public static int accentMid()     { return rgb(155, 130, 225); }
    public static int accentBright()  { return rgb(175, 155, 240); }
    public static int accentLight()   { return rgb(200, 185, 250); }
    public static int accentNeon()    { return rgb(180, 130, 255); }

    // Text colors
    public static int text()          { return rgb(235, 237, 245); }
    public static int textSoft()      { return rgb(190, 195, 215); }
    public static int textMuted()     { return rgb(145, 150, 180); }
    public static int textFaint()     { return rgb(100, 105, 135); }
    public static int textDim()       { return rgb(75, 80, 110); }

    // State colors
    public static int green()         { return rgb(85, 210, 140); }
    public static int greenBright()   { return rgb(110, 230, 160); }
    public static int redSoft()       { return rgb(235, 85, 95); }
    public static int orange()        { return rgb(245, 175, 60); }
    public static int blueSoft()      { return rgb(95, 165, 240); }

    // Utility
    public static int toggleOff()     { return rgb(42, 45, 62); }
    public static int sliderBg()      { return rgb(36, 38, 55); }
    public static int separator()     { return rgb(38, 40, 56); }
    public static int border()        { return rgb(48, 50, 68); }
}
