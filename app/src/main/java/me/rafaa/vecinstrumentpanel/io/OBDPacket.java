package me.rafaa.vecinstrumentpanel.io;

public class OBDPacket {
    public int latency;

    public int engineRpm;
    public int engineLoad;
    public int timingAdvance;

    public int coolantTemp;
    public int intakeTemp;

    public int fuelPressure;
    public int fuelLevel;
    public int ethanolPercentage;

    public int throttle;
    public int mafFlow;

    public int speed;
    
    public static OBDPacket createFromData(String data, int latency) {
        String[] lines = data.split("\n");
        OBDPacket packet = new OBDPacket();

        packet.latency = latency;

        packet.engineRpm = Integer.parseInt(lines[0]);
        packet.engineLoad = Integer.parseInt(lines[1]);
        packet.timingAdvance = Integer.parseInt(lines[2]);

        packet.coolantTemp = Integer.parseInt(lines[3]);
        packet.intakeTemp = Integer.parseInt(lines[4]);

        packet.fuelPressure = Integer.parseInt(lines[5]);
        packet.fuelLevel = Integer.parseInt(lines[6]);
        packet.ethanolPercentage = Integer.parseInt(lines[7]);

        packet.throttle = Integer.parseInt(lines[8]);
        packet.mafFlow = Integer.parseInt(lines[9]);

        packet.speed = Integer.parseInt(lines[10]);

        return packet;
    }
}
