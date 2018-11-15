package org.usfirst.frc.falcons6443.robot.commands.subcommands;

import org.usfirst.frc.falcons6443.robot.commands.SimpleCommand;

/**
 * @author Goirick Saha
 */
public class MoveTurret extends SimpleCommand {

    public MoveTurret(){
        super("Move Turret System");
        requires(turret);
    }

    @Override
    public void initialize() {   }

    @Override
    public void execute() {
        turret.periodic();
    }

    @Override
    public boolean isFinished() {return false;}

    @Override
    public void end(){ turret.off(); }
}
