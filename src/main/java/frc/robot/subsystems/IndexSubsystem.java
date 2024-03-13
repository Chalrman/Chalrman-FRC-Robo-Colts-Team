// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.hardware.TalonFX;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.subsystems.LEDSubsystem.BaseState;

public class IndexSubsystem extends SubsystemBase {
  private final TalonFX indexMotor;
  private final DutyCycleOut indexSpeedDutyCycleOut;
  private final DigitalInput indexSensor;
  private boolean haveNote = false;

  public IndexSubsystem() {
    indexMotor = new TalonFX(Constants.Index.indexMotorID, Constants.Index.indexMotorCanBus);
    indexSpeedDutyCycleOut = new DutyCycleOut(0);
    indexSensor = new DigitalInput(Constants.Index.indexSensorID);
    applyConfigs();
  }

  public boolean haveNote() {
    return !indexSensor.get();
  }

  public void applyConfigs() {
    /* Configure the Index Motor */
    var m_indexConfiguration = new TalonFXConfiguration();
    /* Set Index motor to Brake */
    m_indexConfiguration.MotorOutput.NeutralMode = Constants.Index.motorNeutralValue;
    /* Set the motor direction */
    m_indexConfiguration.MotorOutput.Inverted = Constants.Index.motorOutputInverted;
    /* Config the peak outputs */
    m_indexConfiguration.Voltage.PeakForwardVoltage = Constants.Index.peakForwardVoltage;
    m_indexConfiguration.Voltage.PeakReverseVoltage = Constants.Index.peakReverseVoltage;
    /* Apply Index Motor Configs */
    indexMotor.getConfigurator().apply(m_indexConfiguration);
  }

  public void index() {
    indexMotor.setControl(indexSpeedDutyCycleOut.withOutput(Constants.Index.indexSpeed));
  }

  public void feed() {
    indexMotor.setControl(indexSpeedDutyCycleOut.withOutput(Constants.Index.feedSpeed));
  }

  public void softfeed() {
    indexMotor.setControl(indexSpeedDutyCycleOut.withOutput(Constants.Index.softFeedSpeed));
  }

  public void stop() {
    indexMotor.setControl(indexSpeedDutyCycleOut.withOutput(Constants.Index.stopSpeed));
  }

  public void eject() {
    indexMotor.setControl(indexSpeedDutyCycleOut.withOutput(Constants.Index.ejectSpeed));
  }

  @Override
  public void periodic() {
    boolean currentVal = haveNote();

    if (currentVal != haveNote) {
      haveNote = currentVal;
      LEDSubsystem.setBaseState(haveNote ? BaseState.NOTE : BaseState.EMPTY);
    }
    SmartDashboard.putBoolean("Have note", haveNote);
  }
}