// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import java.util.function.BooleanSupplier;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

public class IndexSubsystem extends SubsystemBase {
  /** Creates a new IndexSubsystem. */
  public static TalonFX indexMotor;
  // public static DigitalInput indexSesnor;
  public IndexSubsystem() {
    indexMotor = new TalonFX(Constants.Index.indexMotorID, Constants.Index.indexMotorCanBus);
    // indexSesnor = new DigitalInput(0);
    applyConfigs();
  }

  public void applyConfigs(){
    /* Configure the Intake Motor */
    var m_indexConfiguration = new TalonFXConfiguration();
    /* Set Intake motor to Brake */
    m_indexConfiguration.MotorOutput.NeutralMode = NeutralModeValue.Brake;
    /* Set the motor direction */
    // m_indexConfiguration.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
    /* Config the peak outputs */
    // m_indexConfiguration.Voltage.PeakForwardVoltage = 12.0;
    // m_indexConfiguration.Voltage.PeakReverseVoltage = -12.0;
    /* Apply Intake Motor Configs */
    indexMotor.getConfigurator().apply(m_indexConfiguration);
  }
  
  public void index() {
    indexMotor.set(-0.80);
  }

  public void feed(){
    Timer.delay(0.75);
    indexMotor.set(-1.0);
    
  }

  public void stop(){
    indexMotor.set(0);
  }

  public void eject(){
    indexMotor.set(1);
  }

  


  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    // SmartDashboard.putBoolean("a", indexSesnor.get());
  }
}