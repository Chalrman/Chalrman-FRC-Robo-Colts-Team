// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix6.configs.Pigeon2Configuration;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Robot;

public class PoseSubsystem extends SubsystemBase {
    private static PoseSubsystem instance;
    private final Swerve s_Swerve;
    private final VisionSubsystem s_Vision;

    private final SwerveDrivePoseEstimator poseEstimator;
    private final Field2d field;
    private static PIDController rotationPID = new PIDController(0.013, 0.0, 0.0); // TODO Constants
    private final Pigeon2 gyro;

    public PoseSubsystem(Swerve s_Swerve, VisionSubsystem s_Vision) {
        assert(instance == null);
        instance = this;
        
        this.s_Swerve = s_Swerve;
        this.s_Vision = s_Vision;

        gyro = new Pigeon2(Constants.Swerve.pigeonID, Constants.Swerve.swerveCanBus);
        gyro.getConfigurator().apply(new Pigeon2Configuration());
        gyro.setYaw(0);        

        Constants.Swerve.rotationPID.enableContinuousInput(-180.0, 180.0);
        Constants.Swerve.rotationPID.setIZone(Constants.Swerve.rotationIZone); // Only use Integral term within this range
        Constants.Swerve.rotationPID.reset();

        poseEstimator = new SwerveDrivePoseEstimator(Constants.Swerve.swerveKinematics, getGyroYaw(), s_Swerve.getModulePositions(), new Pose2d());

        field = new Field2d();
        SmartDashboard.putData("pose/Field", field);

        AutoBuilder.configureHolonomic(
            this::getPose,
            this::setPose,
            s_Swerve::getSpeeds, 
            s_Swerve::driveRobotRelativeAuto,
            // TODO Configure PIDs
            new HolonomicPathFollowerConfig( // HolonomicPathFollowerConfig, this should likely live in your Constants class
                new PIDConstants(8.0, 0.0, 0.0), // Translation PID constants
                new PIDConstants(1.5, 0.0, 0.0), // Rotation PID constants
                Constants.Swerve.maxSpeed, // Max module speed, in m/s
                Constants.Swerve.driveRadius, // Drive base radius in meters. Distance from robot center to furthest module.
                new ReplanningConfig() // Default path replanning config. See the API for the options here
            ),
            Robot::isRed,
            s_Swerve // Reference to Swerve subsystem to set requirements
        );

    }

    public static PoseSubsystem getInstance() {
        return instance;
    }
    
    public Rotation2d getGyroYaw() {
        return Rotation2d.fromDegrees(gyro.getYaw().getValue());
    }

    public void zeroGyro() {
        gyro.setYaw(0);
    }

    public void hack() {
        gyro.setYaw(gyro.getYaw().getValue() + 180.0);
    }

    public Pose2d getPose() {
        return poseEstimator.getEstimatedPosition();
    }

    public void setPose(Pose2d pose) {
        poseEstimator.resetPosition(getGyroYaw(), s_Swerve.getModulePositions(), pose);
    }

    public Rotation2d getHeading() {
        return getPose().getRotation();
    }

    public void setHeading(Rotation2d heading) {
        poseEstimator.resetPosition(getGyroYaw(), s_Swerve.getModulePositions(), new Pose2d(getPose().getTranslation(), heading));
    }

    public void zeroHeading() {
        setHeading(new Rotation2d());
    }

    public Translation2d speakerLocation() {
        return (Robot.isRed() ? Constants.Vision.redSpeakerLocation : Constants.Vision.blueSpeakerLocation);
    }

    public double distanceToSpeaker() {
        double distance = getPose().getTranslation().getDistance(PoseSubsystem.getInstance().speakerLocation()); // distance from center of robot to speaker 
        distance -= Constants.Vision.centerToReferenceOffset; // distance from center of robot to reference point
        //distance *= Constants.Vision.distanceFudgeFactor;
        return distance;
    }

    public Rotation2d dumpShotError() {
        Rotation2d robotAngle = getPose().getRotation();
        if (Robot.isRed()){
            return Constants.Swerve.redDumpAngle.minus(robotAngle);
        } else {
            return Constants.Swerve.dumpAngle.minus(robotAngle);
        }
    }

    public boolean dumpShotAligned() {
        if (Math.abs(dumpShotError().getDegrees()) < Constants.Swerve.maxDumpError) {
            if (Math.abs(Constants.Swerve.rotationPID.getVelocityError()) < Constants.Shooter.dumpShotVelocityErrorMax) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public Rotation2d slideShotError() {
        return Constants.Swerve.slideAngle.minus(getPose().getRotation());
    }

    public boolean slideShotAligned() {
        if (Math.abs(slideShotError().getDegrees()) < Constants.Swerve.maxSlideError) {
            if (Math.abs(Constants.Swerve.rotationPID.getVelocityError()) < Constants.Shooter.slideShotVelocityErrorMax) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static void angleErrorReset() {
        Constants.Swerve.rotationPID.reset();
    }

    private Translation2d speakerOffset() {
        return speakerLocation().minus(getPose().getTranslation());
    }

    private Rotation2d angleToSpeaker() {
        return speakerOffset().getAngle();
    }

    public Rotation2d angleError() {
        Rotation2d speakerAngle = angleToSpeaker();
        Rotation2d robotAngle = getPose().getRotation();

        return speakerAngle.minus(robotAngle);
    }
    
    public static double angleErrorToSpeed(Rotation2d angleError) {
        double angleErrorDeg = angleError.getDegrees();

        return MathUtil.clamp(-rotationPID.calculate(angleErrorDeg), -1.0, 1.0);
    }

    @Override
    public void periodic() {
        poseEstimator.update(getGyroYaw(), s_Swerve.getModulePositions());
        if (DriverStation.isTeleop() || (DriverStation.isAutonomous() && SmartDashboard.getBoolean("Use Vision Pose in Auto", false))) {
            s_Vision.updatePoseEstimate(poseEstimator);
        } else {
            s_Vision.updatePoseEstimate(null);
        }
        field.setRobotPose(getPose());

        SmartDashboard.putNumber("pose/Gyro", getHeading().getDegrees());
        SmartDashboard.putString("pose/Pose", getPose().toString());
    }
}