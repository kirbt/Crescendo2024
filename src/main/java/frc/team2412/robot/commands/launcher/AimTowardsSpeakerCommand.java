package frc.team2412.robot.commands.launcher;

import static frc.team2412.robot.Subsystems.SubsystemConstants.*;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.interpolation.InterpolatingTreeMap;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj2.command.Command;
import frc.team2412.robot.subsystems.DrivebaseSubsystem;
import frc.team2412.robot.subsystems.LauncherSubsystem;
import frc.team2412.robot.util.LauncherDataLoader;
import frc.team2412.robot.util.LauncherDataPoint;
import java.nio.file.FileSystems;

public class AimTowardsSpeakerCommand extends Command {

	private static final InterpolatingTreeMap<Double, LauncherDataPoint> LAUNCHER_DATA =
			LauncherDataLoader.fromCSV(
					FileSystems.getDefault()
							.getPath(
									Filesystem.getDeployDirectory().getPath(),
									LauncherSubsystem.USE_THROUGHBORE
											? "launcher_data_throughbore.csv"
											: "launcher_data_lamprey.csv"));

	private Translation2d SPEAKER_POSITION;

	private DrivebaseSubsystem drivebaseSubsystem;
	private LauncherSubsystem launcherSubsystem;
	private Command yawAlignmentCommand;
	private Rotation2d yawTarget;

	public AimTowardsSpeakerCommand(
			LauncherSubsystem launcherSubsystem, DrivebaseSubsystem drivebaseSubsystem) {
		this.launcherSubsystem = launcherSubsystem;
		this.drivebaseSubsystem = drivebaseSubsystem;
		if (DRIVEBASE_ENABLED) {
			yawAlignmentCommand = drivebaseSubsystem.rotateToAngle(() -> yawTarget, false);
		}

		addRequirements(launcherSubsystem);
	}

	@Override
	public void initialize() {
		SPEAKER_POSITION =
				DriverStation.getAlliance().orElse(Alliance.Red) == Alliance.Blue
						? new Translation2d(0.0, 5.55)
						: new Translation2d(16.5, 5.55);
	}

	@Override
	public void execute() {
		// look ahead half a second into the future
		var fieldSpeed = drivebaseSubsystem.getFieldSpeeds().times(0.5);
		Translation2d robotPosition =
				drivebaseSubsystem
						.getPose()
						.getTranslation()
						.plus(new Translation2d(fieldSpeed.vxMetersPerSecond, fieldSpeed.vyMetersPerSecond));
		Translation2d robotToSpeaker = SPEAKER_POSITION.minus(robotPosition);
		double distance = robotToSpeaker.getNorm();
		LauncherDataPoint dataPoint = LAUNCHER_DATA.get(distance);
		launcherSubsystem.launch(dataPoint.rpm);
		launcherSubsystem.updateDistanceEntry(distance);
	}

	@Override
	public void end(boolean interrupted) {
		yawAlignmentCommand.cancel();
	}

	@Override
	public boolean isFinished() {
		return true;
	}
}
