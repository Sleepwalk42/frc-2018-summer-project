package org.usfirst.frc.falcons6443.robot.commands.subcommands;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.usfirst.frc.falcons6443.robot.commands.SimpleCommand;

/**
 * @author Goirick Saha
 */
public class MoveShooter extends SimpleCommand {

    public MoveShooter() {
        super("Move Shooter System");
        requires(shooter);
        requires(turret);
    }

    @Override
    public void initialize(){ shooter.readyToChargeAnotherBall(); }

    @Override
    public void execute() {
        shooter.autoChargePeriodic();
    }

    @Override
    public boolean isFinished() { return shooter.justShot; }

    @Override
    public void end(){ shooter.off(); }
}