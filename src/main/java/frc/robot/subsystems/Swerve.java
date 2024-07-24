package frc.robot.subsystems;

import frc.robot.SwerveModule;
import frc.robot.Constants;
import frc.robot.Robot;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;

import com.ctre.phoenix6.configs.Pigeon2Configuration;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Swerve extends SubsystemBase {
    private boolean speedLimit = false;

    public SwerveDriveOdometry swerveOdometry;
    public SwerveModule[] mSwerveMods;
    public Pigeon2 gyro;

    private final Field2d field = new Field2d();

    public Swerve() {
        gyro = new Pigeon2(Constants.Swerve.pigeonID, Constants.Swerve.swerveCanBus);
        gyro.getConfigurator().apply(new Pigeon2Configuration());
        gyro.setYaw(0);

        Constants.Swerve.rotationPID.enableContinuousInput(-180.0, 180.0);
        Constants.Swerve.rotationPID.setIZone(Constants.Swerve.rotationIZone); // Only use Integral term within this range
        Constants.Swerve.rotationPID.reset();

        SmartDashboard.putData("swerve/Field", field);
        
        mSwerveMods = new SwerveModule[] {
            new SwerveModule(0, Constants.Swerve.Mod0.constants),
            new SwerveModule(1, Constants.Swerve.Mod1.constants),
            new SwerveModule(2, Constants.Swerve.Mod2.constants),
            new SwerveModule(3, Constants.Swerve.Mod3.constants)
        };
        
        swerveOdometry = new SwerveDriveOdometry(Constants.Swerve.swerveKinematics, getGyroYaw(), getModulePositions());

        AutoBuilder.configureHolonomic(
                this::getPose,
                this::setPose,
                this::getSpeeds, 
                this::driveRobotRelativeAuto,
                new HolonomicPathFollowerConfig( // HolonomicPathFollowerConfig, this should likely live in your Constants class
                    new PIDConstants(8.0, 0.0, 0.0), // Translation PID constants
                    new PIDConstants(1.5, 0.0, 0.0), // Rotation PID constants
                    // TODO Should rotation PID be higher so that we can aim in auto?
                    Constants.Swerve.maxSpeed, // Max module speed, in m/s
                    Constants.Swerve.driveRadius, // Drive base radius in meters. Distance from robot center to furthest module.
                    new ReplanningConfig() // Default path replanning config. See the API for the options here
                ),
                Robot::isRed,
                this // Reference to this subsystem to set requirements
            );
    }

    public void drive(Translation2d translation, double rotation, boolean isOpenLoop) {
        ChassisSpeeds desiredChassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(
                                    translation.getX(), 
                                    translation.getY(), 
                                    rotation, 
                                    getHeading()
                                );

        driveRobotRelative(desiredChassisSpeeds, isOpenLoop);
    }

    public ChassisSpeeds getSpeeds() {
        return Constants.Swerve.swerveKinematics.toChassisSpeeds(getModuleStates());
    }

    public void driveRobotRelativeAuto(ChassisSpeeds desirChassisSpeeds) {
        driveRobotRelative(desirChassisSpeeds, false);
    }

    public void driveRobotRelative(ChassisSpeeds desiredChassisSpeeds, boolean isOpenLoop) {
        ChassisSpeeds.discretize(desiredChassisSpeeds, 0.02); 
        
        SwerveModuleState[] swerveModuleStates = Constants.Swerve.swerveKinematics.toSwerveModuleStates(desiredChassisSpeeds); 
        SwerveDriveKinematics.desaturateWheelSpeeds(swerveModuleStates, Constants.Swerve.maxSpeed);

        for(SwerveModule mod : mSwerveMods) {
            mod.setDesiredState(swerveModuleStates[mod.moduleNumber], isOpenLoop);
        }
    }    

    public SwerveModuleState[] getModuleStates(){
        SwerveModuleState[] states = new SwerveModuleState[4];
        for(SwerveModule mod : mSwerveMods){
            states[mod.moduleNumber] = mod.getState();
        }
        return states;
    }

    public SwerveModulePosition[] getModulePositions(){
        SwerveModulePosition[] positions = new SwerveModulePosition[4];
        for(SwerveModule mod : mSwerveMods){
            positions[mod.moduleNumber] = mod.getPosition();
        }
        return positions;
    }

    public Pose2d getPose() {
        return swerveOdometry.getPoseMeters();
    }

    public void setPose(Pose2d pose) {
        swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), pose);
    }

    public Rotation2d getHeading() {
        return getPose().getRotation();
    }

    public void setHeading(Rotation2d heading) {
        swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), new Pose2d(getPose().getTranslation(), heading));
    }

    public void zeroHeading() {
        setHeading(new Rotation2d());
    }

    public void resetHeading() {
        if (Robot.isRed()) {
            setHeading(new Rotation2d(Math.PI));
        } else {
            setHeading(new Rotation2d());
        }
    }

    public Rotation2d getGyroYaw() {
        return Rotation2d.fromDegrees(gyro.getYaw().getValue());
    }

    public void zeroGyro(){
        gyro.setYaw(0);
    }

    public void resetModulesToAbsolute(){
        for(SwerveModule mod : mSwerveMods){
            mod.resetToAbsolute();
        }
    }

    public Rotation2d dumpShotError() {
        Rotation2d robotAngle = getPose().getRotation();
        if (Robot.isRed()){
            return Constants.Swerve.redDumpAngle.minus(robotAngle);
        } else {
            return Constants.Swerve.blueDumpAngle.minus(robotAngle);
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
        Rotation2d robotAngle = getPose().getRotation();
        if (Robot.isRed()){
            return Constants.Swerve.redSlideAngle.minus(robotAngle);
        } else {
            return Constants.Swerve.blueSlideAngle.minus(robotAngle);
        }
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

    public static double angleErrorToSpeed(Rotation2d angleError) {
        double angleErrorDeg = angleError.getDegrees();

        // System.out.printf("Angle error %01.1f => %01.4f , %01.4f => %01.4f\n", angleErrorDeg, Constants.Swerve.rotationPID.calculate(angleErrorDeg), Constants.Swerve.rotationKS * Math.signum(angleErrorDeg), MathUtil.clamp(-Constants.Swerve.rotationPID.calculate(angleErrorDeg) + -Constants.Swerve.rotationKS * Math.signum(angleErrorDeg), -1.0, 1.0));
        return MathUtil.clamp(-Constants.Swerve.rotationPID.calculate(angleErrorDeg) + Constants.Swerve.rotationKS * Math.signum(angleErrorDeg), -1.0, 1.0);
    }

    public void enableSpeedLimit() {
        speedLimit = true;
    }

    public void disableSpeedLimit() {
        speedLimit = false;
    }

    public double getSpeedLimitRot() {
        return speedLimit ? Constants.Swerve.speedLimitRot : 1.0;
    }

    public void setMotorsToCoast(){
        for(SwerveModule mod : mSwerveMods){
            mod.setCoastMode();  
        }
    }

    public void setMotorsToBrake(){
        for(SwerveModule mod : mSwerveMods){
            mod.setCoastMode();  
        }
    }

    @Override
    public void periodic(){
        swerveOdometry.update(getGyroYaw(), getModulePositions());

        for(SwerveModule mod : mSwerveMods){
            SmartDashboard.putNumber("Swerve/Mod/" + mod.moduleNumber + " CANcoder", mod.getCANcoder().getDegrees());
            SmartDashboard.putNumber("Swerve/Mod/" + mod.moduleNumber + " Angle", mod.getPosition().angle.getDegrees());
            SmartDashboard.putNumber("Swerve/Mod/" + mod.moduleNumber + " Velocity", mod.getState().speedMetersPerSecond);    
        }

        SmartDashboard.putNumber("Gyro", getHeading().getDegrees());
        // System.out.println("Swerve: Heading @ " + getHeading().getDegrees());
        SmartDashboard.putString("swerve/Pose", getPose().toString());
        field.setRobotPose(getPose());
    }
}