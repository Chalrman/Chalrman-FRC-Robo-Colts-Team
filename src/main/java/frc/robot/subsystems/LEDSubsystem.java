// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix.led.CANdle;
import com.ctre.phoenix.led.RainbowAnimation;
import com.ctre.phoenix.led.CANdle.LEDStripType;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class LEDSubsystem extends SubsystemBase {
  /** Creates a new LEDSubsystem. */
  private IndexSubsystem m_Index;
  private CANdle m_candle = new CANdle(0, "rio");

  public static class Color {
    private final int R, G, B;
    public Color(int r, int g, int b) {
      R = r;
      G = g;
      B = b;
    }
  }

  public LEDSubsystem() {
    m_Index = new IndexSubsystem();
    m_candle.configBrightnessScalar(0.50);
    m_candle.configLEDType(LEDStripType.GRB);
    m_candle.configV5Enabled(true);
    m_candle.configLOSBehavior(true);
  }
  
  public void setRainbow(){
    m_candle.clearAnimation(0);
    m_candle.animate(new RainbowAnimation(0.50, 0.5, 68, false, 8));
  }

  public void detectIntake(){
    if (m_Index.getIndexSensor().getAsBoolean()) {
      setRainbow();
    } else {
      m_candle.setLEDs(255, 0, 0, 255, 0, 31);
    }
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    m_candle.setLEDs(255, 0, 0, 255, 0, 31);
  }
}