package org.usfirst.frc.falcons6443.robot.commands;

import org.usfirst.frc.falcons6443.robot.Robot;
import org.usfirst.frc.falcons6443.robot.hardware.joysticks.Xbox;
import org.usfirst.frc.falcons6443.robot.hardware.pneumatics.SingularCompressor;
import org.usfirst.frc.falcons6443.robot.utilities.enums.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Teleoperated mode for the robot.
 * The execute method of this class handles all possible inputs from the driver during the game.
 */
public class TeleopMode extends SimpleCommand {

    private int unpressedID = 0;
    private boolean first = true;

    private Xbox primary;           //Drive and flywheel/output
    private Xbox secondary;         //Secondary functions
    private List<Boolean> runOnceSavedData = new ArrayList<>();
    private List<Boolean> isManualLessThanBuffer = new ArrayList<>();
    private List<Callable<Boolean>> isManualGetter = new ArrayList<>(); //add control manual getters
    private List<Consumer<Boolean>> isManualSetter = new ArrayList<>(); //add control manual setters
    //private WCDProfile driveProfile;//Profile used to calculate robot drive power

    public TeleopMode() {
        super("Teleop Command");
        requires(driveTrain);
        requires(shooter);
        requires(turret);
        requires(intake);
    }

    //A list of all manual controls of the robot, excluding drive
    //Used for manual controls. Can only have one ManualControls per manual axis (NOT per subsystem!)
    public enum ManualControls {
        Turret, Shooter, Intake
    }

    @Override
    public void initialize() {
        primary = Robot.oi.getXbox(true);
        secondary = Robot.oi.getXbox(false);
        //driveProfile = new FalconDrive(primary);

        //add manual getters and setters using isManualGetterSetter
        while(isManualGetter.size() < ManualControls.values().length) isManualGetter.add(null); //ensures that array is at least size of ManualControls enum
        while(isManualSetter.size() < ManualControls.values().length) isManualSetter.add(null);
        while(isManualLessThanBuffer.size() < ManualControls.values().length) isManualLessThanBuffer.add(null);
        addIsManualGetterSetter(ManualControls.Shooter, () -> shooter.getManual(), (Boolean set) ->  shooter.setManual(set));
        addIsManualGetterSetter(ManualControls.Turret, () -> turret.getManual(), (Boolean set) ->  turret.setManual(set));
        addIsManualGetterSetter(ManualControls.Intake, () -> intake.getManual(), (Boolean set) -> intake.setManual(set));
    }

    @Override
    public void execute() {

        //drive
        //driveTrain.falconDrive(primary.leftStickX(), primary.rightTrigger(), primary.leftTrigger());
        // driveTrain.tankDrive(driveProfile.calculate()); TODO: TEST this cause profiles are cool

        //shooter
      //  press(primary.leftBumper(), () -> shooter.charge());
      //  runOncePerPress(primary.rightBumper(), () -> shooter.shoot(), true); //resets the dashboard Load boolean

        shooter.manual(primary.leftStickY());
       // System.out.println("SHOOTER RATE: " + shooter.getRate());
        System.out.println("SHOOTER revs: " + shooter.encoder.getRevs());
        System.out.println("SHOOTER tics: " + shooter.encoder.get());
        System.out.println("SHOOTER revs per minute: " + shooter.getRate());

        press(primary.Y(), () -> shooter.encoder.reset());

        //turret
        //   runOncePerPress(primary.eight(), () -> turret.disableToggle(), false);
        //   runOncePerPress(primary.Y(), () -> turret.roamingToggle(), false);

        turret.manual(primary.rightStickY());
        System.out.println("Turret encoder: " + turret.encoder.get());

        //intake
        //press(primary.X(), () -> intake.intake());
      //  manual(ManualControls.Intake, primary.rightTrigger(), () -> intake.manual(primary.rightTrigger()));
        press(primary.B(), () -> intake.movePistonIn());
        press(primary.A(), () -> intake.movePistonOut());

        press(primary.X(), () -> SingularCompressor.get().start());

        //off
     //   off(() -> shooter.off(), ManualControls.Shooter, primary.leftBumper());
     //   off(() -> turret.off(), ManualControls.Turret, primary.eight(), primary.Y());
     //   off(() -> intake.off(), ManualControls.Intake, primary.X());
        off(() -> SingularCompressor.get().stop(), primary.X());

        //general periodic functions
        turret.update(primary.A());
        periodicEnd();

        //other junk
        if(shooter.isCharged()) primary.setRumble(XboxRumble.RumbleBoth, 0.4);
    }

    //adding manual getters and setters to Lists using params:
    // ManualControls.manualEnum, () -> function(), (Boolean set) -> function(set)
    //Example: addIsManualGetter(TeleopStructure.ManualControls.Elevator, () -> elevator.getManual(),
    //                      (Boolean set) -> elevator.setManual(set));
    //also adds isManualLessThanBuffer to ensure equal numbers of getters/setters to buffer checkers
    private void addIsManualGetterSetter(ManualControls manual, Callable<Boolean> callable,
                                         Consumer<Boolean> consumer) {
        isManualGetter.add(manual.ordinal(), callable);
        isManualSetter.add(manual.ordinal(), consumer);
        isManualLessThanBuffer.add(manual.ordinal(), true);
    }

    //Pairs an action with a button
    private void press(boolean button, Runnable action){
        if(button) action.run();
    }

    //Pairs an action with a button, compatible with manual()
    // ie: this function can be used with manual() to control the same component
    // eg: button control and (backup) manual control of the same component
    private void press(ManualControls manual, boolean button, Runnable action){
        if(button) {
            isManualSetter.get(manual.ordinal()).accept(false); //turn manual off if nonmanual button pressed
            action.run();
        }
    }

    //Pairs an action with a manual input (joystick, trigger, etc)
    private void manual(ManualControls manual, double input, Runnable action){
        if(Math.abs(input) > 0.2){
            isManualSetter.get(manual.ordinal()).accept(true);
            isManualLessThanBuffer.set(manual.ordinal(), false);
            action.run();
        } else {
            isManualLessThanBuffer.set(manual.ordinal(), true);
        }
    }

    //Runs an action when a set of buttons is not pressed
    private void off(Runnable off, boolean ... button){
        if(areAllFalse(button)) off.run();
    }

    //Runs an action when manual is less than buffer
    private void off(Runnable off, ManualControls manualNumber) {
        if(isManualLessThanBuffer.get(manualNumber.ordinal())) off.run();
    }

    //Runs an action when a set of buttons is not pressed and manual is less than buffer
    private void off(Runnable off, ManualControls manualNumber, boolean ... button){
        try {
            if(areAllFalse(button) && !isManualGetter.get(manualNumber.ordinal()).call()) off.run();
            else if((areAllFalse(button) && isManualGetter.get(manualNumber.ordinal()).call()
                    && isManualLessThanBuffer.get(manualNumber.ordinal()))) off.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Pairs an action with a button, activated only once unpressed (true) or once pressed (false)
    //This action will only run once, unlike press() which runs periodically until unpressed
    private void runOncePerPress(boolean button, Runnable function, boolean unpressedMode){
        if(first) runOnceSavedData.add(unpressedID, false);
        if(button){
            if(!unpressedMode && !runOnceSavedData.get(unpressedID)){
                function.run();
            }
            runOnceSavedData.set(unpressedID, true);
        } else {
            if(unpressedMode && runOnceSavedData.get(unpressedID)){
                function.run();
            }
            runOnceSavedData.set(unpressedID, false);
        }
        unpressedID++;
    }

    //clears the unpressedID
    private void periodicEnd(){
        first = false;
        unpressedID = 0;
    }

    private boolean areAllFalse(boolean[] array) {
        for (boolean b : array) if (b) return false;
        return true;
    }

    private static boolean areAllZero(double buffer, double[] array) {
        for (double d : array) if (d > buffer) return false;
        return true;
    }

    public void reset() {
        //driveProfile = null;
    }

    public boolean isFinished() {
        return false;
    }
}